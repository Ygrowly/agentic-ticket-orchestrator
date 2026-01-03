package com.iflytek.astron.link.controller.vo.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ToolManagerRequest {
    private ToolManagerHeader header;

    private Payload payload;

    public record ToolManagerHeader(@JsonProperty("app_id") String appId) {
    }


    public record Payload(List<SchemaInfo> tools) {
    }


    @Data
    public static class SchemaInfo {
        private String id;
        private String name;
        private String version;
        private String description;
        @JsonProperty("schema_type")
        private Integer schemaType;
        @JsonProperty("openapi_schema")
        private String openapiSchema;
    }
}