package com.iflytek.astron.link.controller.vo.res;

import com.google.common.base.Joiner;
import com.iflytek.astron.link.constant.LinkErrorCode;
import lombok.Data;

import java.util.Map;

@Data
public class ToolManagerResponse {
    private int code;
    private String message;
    private String sid;
    private Map<String, Object> data;

    public static ToolManagerResponse response(LinkErrorCode code) {
        ToolManagerResponse response = new ToolManagerResponse();
        response.setCode(code.getCode());
        response.setMessage(code.getMessage());
        response.setSid("sid-" + System.currentTimeMillis());
        return response;
    }

    public static ToolManagerResponse success(Map<String, Object> data) {
        ToolManagerResponse response = response(LinkErrorCode.SUCCESSES);
        response.setData(data);
        return response;
    }

    public static ToolManagerResponse error(LinkErrorCode code, String... msg) {
        ToolManagerResponse response = response(code);
        if (msg.length > 0) {
            response.setMessage(code.getMessage() + " " + Joiner.on(",").join(msg));
        }
        response.setData(Map.of());
        return response;
    }
}