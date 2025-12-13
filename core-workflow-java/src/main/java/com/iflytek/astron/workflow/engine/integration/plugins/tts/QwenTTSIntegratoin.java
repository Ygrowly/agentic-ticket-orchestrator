package com.iflytek.astron.workflow.engine.integration.plugins.tts;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.util.S3ClientUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于阿里的千问tts实现语言合成
 *
 * @author YiHui
 * @date 2025/12/13
 */
@Slf4j
@Component
public class QwenTTSIntegratoin implements TtsIntegration {
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.model}")
    private String model;

    @Value("${qwen.tts-url}")
    private String url;

    @Value("${qwen.source:qwen}")
    private String source;

    @Resource
    private S3ClientUtil s3ClientUtil;

    @Override
    public String source() {
        return source;
    }

    @Override
    public Map<String, Object> call(NodeState nodeState, Map<String, Object> inputs) throws Exception {
        Node node = nodeState.node();
        log.info("Executing Smart TTS node: {}", node.getId());

        // Extract parameters
        String text = getString(inputs, "text");
        String vcn = getString(inputs, "vcn");
        Integer speed = getInteger(inputs, "speed", 50);

        // Validate required parameters
        if (text == null || text.isEmpty()) throw new IllegalArgumentException("Text is required");

        if (vcn == null || vcn.isEmpty()) {
            throw new IllegalArgumentException("Voice character (vcn) is required");
        }

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .text(text)
                .voice(getVoice(vcn))
                .build();

        MultiModalConversationResult result = conv.call(param);
        String resUrl = result.getOutput().getAudio().getUrl();
        log.info("TTS生成链接: {}", resUrl);

        // Perform Smart TTS synthesis
        byte[] audioData = downloadAudioFromUrl(resUrl);

        // Upload audio file to Minio and get URL
        String objectKey = "audio/" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + UUID.randomUUID() + ".wav";
        // 将音频文件下载到字节流中，然后上传到 minio
        String audioUrl = s3ClientUtil.uploadObject(objectKey, "audio/wav", audioData);

        // Create result
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("data", Map.of("voice_url", audioUrl));
        outputs.put("code", 0);
        outputs.put("message", "Success");
        outputs.put("sid", EngineContextHolder.get().getSid());

        log.info("Smart TTS node completed: {}", node.getId());
        return outputs;
    }

    private AudioParameters.Voice getVoice(String vcn) {
        for (AudioParameters.Voice vo : AudioParameters.Voice.values()) {
            if (vo.name().equalsIgnoreCase(vcn) || vo.getValue().equalsIgnoreCase(vcn)) {
                return vo;
            }
        }
        return AudioParameters.Voice.CHERRY;
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

    private byte[] downloadAudioFromUrl(String audioUrl) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(audioUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("下载音频文件失败: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException("音频文件内容为空");
            }

            return responseBody.bytes();
        }
    }

}
