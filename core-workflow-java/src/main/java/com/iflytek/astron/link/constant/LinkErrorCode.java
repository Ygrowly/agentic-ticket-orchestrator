package com.iflytek.astron.link.constant;

/**
 * 错误码枚举类
 */
public enum LinkErrorCode {
    SUCCESSES(0, "Success"),
    COMMON_ERR(1000, "Common error"),
    JSON_PROTOCOL_PARSER_ERR(1001, "JSON protocol parser error"),
    JSON_SCHEMA_VALIDATE_ERR(1002, "JSON schema validate error"),
    TOOL_NOT_EXIST_ERR(1003, "Tool does not exist"),
    OPERATION_ID_NOT_EXIST_ERR(1004, "Operation ID does not exist"),
    OPENAPI_AUTH_TYPE_ERR(1005, "OpenAPI auth type error"),
    RESPONSE_SCHEMA_VALIDATE_ERR(1006, "Response schema validate error"),
    NO_TOOL_ID_PROVIDER(400, "No tool IDs provided"),
    INVALID_TOOL_ID_FORMAT(400, "Invalid tool ID format: "),
    UNKNOWN_ERR(500, "Unknown error"),
    ;

    private final int code;
    private final String message;

    LinkErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}