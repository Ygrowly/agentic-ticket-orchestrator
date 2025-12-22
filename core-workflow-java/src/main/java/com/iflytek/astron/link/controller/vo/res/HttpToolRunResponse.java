package com.iflytek.astron.link.controller.vo.res;

import lombok.Data;

import java.util.Map;

@Data
public class HttpToolRunResponse {
    private HttpRunResponseHeader header;
    private Map<String, Object> payload;

    @Data
    public static class HttpRunResponseHeader {
        private int code;
        private String message;
        private String sid;
    }
}