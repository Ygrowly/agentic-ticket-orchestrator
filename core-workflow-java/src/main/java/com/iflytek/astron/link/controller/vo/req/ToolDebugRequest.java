package com.iflytek.astron.link.controller.vo.req;

import lombok.Data;

import java.util.Map;

@Data
public class ToolDebugRequest {
    private String server;
    private String method;
    private Map<String, Object> path;
    private Map<String, Object> query;
    private Map<String, Object> header;
    private Map<String, Object> body;
    private String openapiSchema;
}