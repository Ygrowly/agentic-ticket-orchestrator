package com.iflytek.astron.workflow.engine.integration.plugins.tts;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.util.S3ClientUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Smart TTS node executor
 * Implements human-like speech synthesis functionality using iFlyTek's Smart TTS service
 */
@Slf4j
@Component
public class SmartTTSIntegration implements TtsIntegration {
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Value("${spark.app-id}")
    private String appId;

    @Value("${spark.api-key}")
    private String apiKey;

    @Value("${spark.api-secret}")
    private String apiSecret;

    @Value("${spark.tts-url}")
    private String ttsUrl;

    @Value("${spark.source:spark}")
    private String source;

    @Resource
    private S3ClientUtil s3ClientUtil;

    @Override
    public String source() {
        return source;
    }

    public Map<String, Object> call(NodeState nodeState, Map<String, Object> inputs) throws Exception {
        Node node = nodeState.node();
        log.info("Executing Smart TTS node: {}", node.getId());

        // Extract parameters
        String text = getString(inputs, "text");
        String vcn = getString(inputs, "vcn");
        Integer speed = getInteger(inputs, "speed", 50);

        // Validate required parameters
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text is required");
        }

        if (vcn == null || vcn.isEmpty()) {
            throw new IllegalArgumentException("Voice character (vcn) is required");
        }

        // Perform Smart TTS synthesis
        byte[] audioData = performSmartTTSSynthesis(text, vcn, speed, appId, apiKey, apiSecret);

        // Upload audio file to Minio and get URL
        String objectKey = "audio/" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + UUID.randomUUID() + ".mp3";
        String audioUrl = s3ClientUtil.uploadObject(objectKey, "audio/mpeg", audioData);

        // Create result
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("data", Map.of("voice_url", audioUrl));
        outputs.put("code", 0);
        outputs.put("message", "Success");
        outputs.put("sid", EngineContextHolder.get().getSid());

        log.info("Smart TTS node completed: {}", node.getId());
        return outputs;
    }

    /**
     * Performs Smart TTS synthesis using WebSocket connection
     *
     * @param text      Text to synthesize
     * @param vcn       Voice character name
     * @param speed     Speech speed (1-100)
     * @param appId     Application ID
     * @param apiKey    API Key
     * @param apiSecret API Secret
     * @return Audio data as byte array
     * @throws Exception if synthesis fails
     */
    private byte[] performSmartTTSSynthesis(String text, String vcn, Integer speed, String appId, String apiKey, String apiSecret) throws Exception {
        log.info("Performing Smart TTS synthesis for text: {} with voice: {}, speed: {}",
                text.substring(0, Math.min(text.length(), 50)) + (text.length() > 50 ? "..." : ""),
                vcn, speed);

        // Generate authenticated WebSocket URL
        String authUrl = getAuthUrl(ttsUrl, apiKey, apiSecret);
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");

        // Create WebSocket listener to handle TTS response
        CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();
        TTSWebSocketListener listener = new TTSWebSocketListener(resultFuture, text, vcn, speed, appId);

        // Establish WebSocket connection
        Request request = new Request.Builder().url(wsUrl).build();
        WebSocket webSocket = httpClient.newWebSocket(request, listener);

        // Wait for result or timeout
        try {
            return resultFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            webSocket.close(1000, "Timeout or error");
            throw new RuntimeException("TTS synthesis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates authenticated URL for WebSocket connection
     */
    private String getAuthUrl(String requestUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(requestUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        String signatureOrigin = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] signData = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signData);

        String authorizationOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        return requestUrl + "?authorization=" + URLEncoder.encode(authorization, StandardCharsets.UTF_8.name()) +
                "&date=" + URLEncoder.encode(date, StandardCharsets.UTF_8.name()) +
                "&host=" + URLEncoder.encode(url.getHost(), StandardCharsets.UTF_8.name());
    }

    /**
     * WebSocket listener for handling TTS service responses
     */
    private static class TTSWebSocketListener extends WebSocketListener {
        private final CompletableFuture<byte[]> resultFuture;
        private final String text;
        private final String vcn;
        private final Integer speed;
        private final String appId;
        private final ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

        public TTSWebSocketListener(CompletableFuture<byte[]> resultFuture, String text, String vcn, Integer speed, String appId) {
            this.resultFuture = resultFuture;
            this.text = text;
            this.vcn = vcn;
            this.speed = speed;
            this.appId = appId;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            // Send TTS request when connection is established
            JSONObject requestJson = buildTTSRequest();
            webSocket.send(requestJson.toString());
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject responseJson = JSON.parseObject(text);
                if (log.isDebugEnabled()) {
                    log.debug("TTS service response: {}", responseJson);
                }

                int code = responseJson.getJSONObject("header").getIntValue("code");

                if (code != 0) {
                    String message = responseJson.getJSONObject("header").getString("message");
                    resultFuture.completeExceptionally(new RuntimeException("TTS service error: " + code + " - " + message));
                    webSocket.close(1000, "Error");
                    return;
                }

                if (responseJson.containsKey("payload")) {
                    JSONObject payload = responseJson.getJSONObject("payload");
                    if (payload.containsKey("audio")) {
                        String audioBase64 = payload.getJSONObject("audio").getString("audio");
                        byte[] audioData = Base64.getDecoder().decode(audioBase64);
                        audioStream.write(audioData);
                    }

                    int status = payload.getJSONObject("audio").getIntValue("status");
                    if (status == 2) {
                        // Synthesis completed
                        resultFuture.complete(audioStream.toByteArray());
                        webSocket.close(1000, "Completed");
                    }
                }
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
                webSocket.close(1000, "Error");
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            resultFuture.completeExceptionally(t);
        }

        /**
         * Builds the TTS request JSON
         */
        private JSONObject buildTTSRequest() {
            JSONObject request = new JSONObject();

            // Header
            JSONObject header = new JSONObject();
            header.put("app_id", appId);
            header.put("status", 2); // 3 means end of input
            request.put("header", header);

            // Parameter
            JSONObject parameter = new JSONObject();
            JSONObject tts = new JSONObject();
            tts.put("vcn", vcn);
            tts.put("speed", speed);
            tts.put("volume", 50);
            tts.put("pitch", 50);
            tts.put("bgs", 0);
            tts.put("rhy", 0);
            tts.put("reg", 0);
            tts.put("rdn", 0);

            JSONObject audio = new JSONObject();
            audio.put("encoding", "lame");
            audio.put("sample_rate", 24000);
            audio.put("channels", 1);
            audio.put("bit_depth", 16);
            audio.put("frame_size", 0);
            tts.put("audio", audio);
            parameter.put("tts", tts);
            request.put("parameter", parameter);

            // Payload
            JSONObject payload = new JSONObject();
            JSONObject textPayload = new JSONObject();
            textPayload.put("encoding", "utf8");
            textPayload.put("compress", "raw");
            textPayload.put("format", "plain");
            textPayload.put("status", 2); // 3 means end of input
            textPayload.put("seq", 0);
            textPayload.put("text", Base64.getEncoder().encodeToString(this.text.getBytes(StandardCharsets.UTF_8)));
            payload.put("text", textPayload);
            request.put("payload", payload);

            return request;
        }
    }

    public String getString(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // Simple ByteArrayOutputStream implementation for audio data
    private static class ByteArrayOutputStream {
        private byte[] buf = new byte[1024];
        private int count = 0;

        public synchronized void write(byte[] b) {
            ensureCapacity(count + b.length);
            System.arraycopy(b, 0, buf, count, b.length);
            count += b.length;
        }

        public synchronized byte[] toByteArray() {
            return Arrays.copyOf(buf, count);
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity - buf.length > 0) {
                grow(minCapacity);
            }
        }

        private void grow(int minCapacity) {
            int oldCapacity = buf.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            buf = Arrays.copyOf(buf, newCapacity);
        }
    }

    // Simple URL class for parsing URL components
    private static class URL {
        private final String url;
        private final String host;
        private final String path;

        public URL(String url) throws Exception {
            this.url = url;
            int protocolEnd = url.indexOf("://");
            if (protocolEnd == -1) {
                throw new IllegalArgumentException("Invalid URL: " + url);
            }
            int hostStart = protocolEnd + 3;
            int hostEnd = url.indexOf("/", hostStart);
            if (hostEnd == -1) {
                this.host = url.substring(hostStart);
                this.path = "/";
            } else {
                this.host = url.substring(hostStart, hostEnd);
                this.path = url.substring(hostEnd);
            }
        }

        public String getHost() {
            return host;
        }

        public String getPath() {
            return path;
        }
    }
}