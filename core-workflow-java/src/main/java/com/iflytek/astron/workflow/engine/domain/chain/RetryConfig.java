package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author YiHui
 * @date 2025/12/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {

    /**
     * 是否需要重试
     */
    @JsonProperty("shouldRetry")
    private Boolean shouldRetry;

    /**
     * 重试次数
     */
    @JsonProperty("maxRetries")
    private Integer maxRetries;

    /**
     * 错误处理策略
     */
    @JsonProperty("errorStrategy")
    private Integer errorStrategy;

    /**
     * 超时时间，秒为单位； 不存在或者为0的时候，表示设置超时时间
     */
    @JsonProperty("timeout")
    private Float timeout;

    /**
     * 对于错误策略为 错误码的场景，这里存储的是预设的返回内容，用于构建下一个节点的输入
     */
    @JsonProperty("customOutput")
    private Map<String, Object> customOutput;

    /**
     * 重试策略：0-固定间隔，1-线性退避，2-指数退避
     */
    @JsonProperty("retryStrategy")
    private Integer retryStrategy;

    /**
     * 重试间隔，单位秒
     */
    @JsonProperty("retryInterval")
    private Float retryInterval;


    public boolean timeOutEnabled() {
        return timeout != null && timeout > 0.001;
    }

    public long toMillis() {
        return (long) (timeout * 1000);
    }
}
