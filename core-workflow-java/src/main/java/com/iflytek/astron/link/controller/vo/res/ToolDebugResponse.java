package com.iflytek.astron.link.controller.vo.res;

import lombok.Data;

import java.util.Map;

@Data
public class ToolDebugResponse {
    private ToolDebugResponseHeader header;
    private Map<String, Object> payload;

    @Data
    public static class ToolDebugResponseHeader {
        private int code;
        private String message;
        private String sid;
    }
}