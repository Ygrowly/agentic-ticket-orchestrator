package com.iflytek.astron.workflow.controller.vo;

import lombok.Data;

import java.util.UUID;

/**
 * Unified response entity
 */
@Data
public class RspVo<T> {
    private int code;
    private String message;
    private T data;
    private String sid;

    public RspVo() {}

    public RspVo(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public RspVo(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public RspVo(int code, String message, T data, String sid) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.sid = sid;
    }

    public static <T> RspVo<T> success(T data) {
        return new RspVo<>(0, "success", data);
    }

    public static <T> RspVo<T> success(T data, String sid) {
        return new RspVo<>(0, "success", data, sid);
    }

    public static <T> RspVo<T> error(int code, String message) {
        return new RspVo<>(code, message);
    }

    public static <T> RspVo<T> error(int code, String message, String sid) {
        RspVo<T> response = new RspVo<>(code, message);
        response.setSid(sid);
        return response;
    }

    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * Generate a unique session ID
     *
     * @return Generated SID
     */
    public static String generateSid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}