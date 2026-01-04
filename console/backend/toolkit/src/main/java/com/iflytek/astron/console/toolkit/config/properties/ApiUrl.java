package com.iflytek.astron.console.toolkit.config.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;


/**
 * @program: AICloud-Customer-Service-Robot
 * @description: Remote API address configuration class
 * @create: 2020-10-23 15:16
 */
@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "api.url")
public class ApiUrl {
    String defaultAddRepo;
    String knowledgeUrl;
    String streamChatUrl;
    String toolUrl;
    String toolRpaUrl;
    String appUrl;
    String apiKey;
    String apiSecret;
    String workflow;
    String openPlatform;
    String tenantId;
    String tenantKey;
    String tenantSecret;
    /**
     * Teacher Zhang's MCP server address
     */
    String mcpToolServer;

    String mcpAuthServer;
    String mcpUrlServer;
    String sparkDB;

    // Get fine-tuning model authentication parameters
    String modelAk;
    String modelSk;
    String localModel;
    String datasetUrl;
    String datasetFileUrl;
    String xinghuoDatasetFileUrl;
    String deleteXinghuoDatasetFileUrl;
    String deleteXinghuoDatasetUrl;
    String rpaUrl;

    private volatile String realToolUrl;

    private void autoInitRealToolUrl() {
        if (realToolUrl != null) {
            return;
        }

        // 因为我们在core-workflow-java中也实现了工具服务，因此当配置中的toolUrl 直接是 workflow，那么直接使用即可
        if (Objects.equals(toolUrl, workflow)) {
            realToolUrl = toolUrl;
            return;
        }

        // 如果配置的是独立toolUrl，通常是指向python版本link，此时我们需要先试探一下，python版本的服务是否启动；如果没有启动，则使用workflow的服务；否则直接使用python link服务
        // 通过 http 请求 toolUrl，设置超时时间为 50ms
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(toolUrl + "/health")).timeout(Duration.ofMillis(50)).build();
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(50)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // 若能收到响应，则说明服务可用
            realToolUrl = toolUrl;
        } catch (Exception e) {
            log.warn("Python link service is not available, using workflow service instead", e);
            // 出现异常表示服务不可达，降级到 workflow 服务
            realToolUrl = workflow;
        }
        log.info("Using toolUrl: {}", realToolUrl);
    }

    /**
     * 获取工具服务地址
     * <p>
     * - 在这里做了一个降级逻辑，当配置的toolUrl 是 workflow 时，则直接使用workflow，否则尝试使用python版本link，如果python版本link启动了，则使用python版本link，否则使用workflow的地址
     *
     * @return
     */
    public String getToolUrl() {
        this.autoInitRealToolUrl();
        return realToolUrl;
    }


    /**
     * 自动转换工具服务地址
     * <p>
     * - 当配置的toolUrl 是 workflow 时，则直接使用workflow，否则尝试使用python版本link，如果python版本link启动了，则使用python版本link，否则使用workflow的地址
     *
     * @param toolEndPoint
     * @return
     */
    public String autoTransferToolUrl(String toolEndPoint) {
        this.autoInitRealToolUrl();

        // 直接使用python版本link
        if (Objects.equals(realToolUrl, toolUrl)) {
            return toolEndPoint;
        }

        // 使用java版本的link服务
        if (toolEndPoint.contains("http://core-aitools") || toolEndPoint.contains(toolUrl)) {
            // 表示需要进行域名转换
            try {
                URI originalUri = new URI(toolEndPoint);
                URI targetUri = new URI(realToolUrl);

                // 重建 URL：使用目标 URL 的协议、主机和端口，保持原 URL 的路径等信息
                URI newUri = new URI(
                        targetUri.getScheme(),
                        targetUri.getUserInfo(),
                        targetUri.getHost(),
                        targetUri.getPort(),
                        originalUri.getPath(),
                        originalUri.getQuery(),
                        originalUri.getFragment()
                );
                return newUri.toString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return toolEndPoint;
    }
}
