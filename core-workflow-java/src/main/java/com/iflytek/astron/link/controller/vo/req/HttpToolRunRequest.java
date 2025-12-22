package com.iflytek.astron.link.controller.vo.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class HttpToolRunRequest {
    private HttpRunHeader header;
    private HttpRunParameter parameter;
    private HttpRunPayload payload;

    public record HttpRunHeader(
            @JsonProperty("app_id")
            String appId,
            String uid) {
    }

    public record HttpRunParameter(
            @JsonProperty("tool_id")
            String toolId,
            @JsonProperty("operation_id")
            String operationId,
            @JsonProperty("version")
            String version) {
    }

    public record HttpRunPayload(Map<String, Object> message) {
    }
}