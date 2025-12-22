package com.iflytek.astron.link.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class HttpExecutor {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String doCall(String server, String method, Map<String, Object> path,
                         Map<String, Object> query, Map<String, Object> header,
                         Map<String, Object> body) throws Exception {
        // 构建完整的URL
        String url = buildUrl(server, path, query);

        // 构建请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

        // 设置请求头
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue().toString());
            }
        }

        // 设置请求方法和体
        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.GET();
                break;
            case "POST":
                if (body != null) {
                    String jsonBody = objectMapper.writeValueAsString(body);
                    requestBuilder.header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                } else {
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                }
                break;
            case "PUT":
                if (body != null) {
                    String jsonBody = objectMapper.writeValueAsString(body);
                    requestBuilder.header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                } else {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                }
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // 发送请求
        HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private String buildUrl(String server, Map<String, Object> path, Map<String, Object> query) {
        // 处理路径参数
        String url = server;
        if (path != null) {
            for (Map.Entry<String, Object> entry : path.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}",
                        URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
        }

        // 处理查询参数
        if (query != null && !query.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) +
                        "=" + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
            url += "?" + joiner.toString();
        }

        return url;
    }
}