package com.iflytek.astron.workflow.exception;

import lombok.Getter;

/**
 * Error code enumeration
 */
@Getter
public enum ErrorCode {
    Success(0, "Success"),
    NODE_RUN_ERROR(1001, "Node run error"),
    WORKFLOW_EXECUTION_ERROR(1002, "Workflow execution error"),
    INVALID_NODE_CONFIGURATION(1003, "Invalid node configuration"),
    MISSING_DEPENDENCY(1004, "Missing dependency"),
    TIMEOUT_ERROR(1005, "Execution timeout"),
    INTERRUPTED_ERROR(1006, "Execution interrupted"),

    // Parameter errors
    PARAM_ERROR(460, "Parameter validation error"),

    // Application errors
    APP_NOT_FOUND_ERROR(20000, "Application not found"),

    // Protocol errors
    PROTOCOL_VALIDATION_ERROR(20100, "Protocol validation failed"),
    PROTOCOL_BUILD_ERROR(20101, "Protocol build failed"),
    PROTOCOL_CREATE_ERROR(20102, "Protocol creation error"),
    PROTOCOL_DELETE_ERROR(20103, "Protocol deletion error"),
    PROTOCOL_UPDATE_ERROR(20104, "Protocol update failed"),

    // Flow errors
    FLOW_NOT_FOUND_ERROR(20201, "Flow ID not found"),
    FLOW_ID_TYPE_ERROR(20202, "Invalid Flow ID"),
    FLOW_NOT_PUBLISH_ERROR(20204, "Workflow not published"),
    FLOW_NO_APP_ID_ERROR(20205, "Workflow has no appid"),
    FLOW_PUBLISH_ERROR(20206, "Workflow publish failed"),
    FLOW_VERSION_ERROR(20208, "Flow version not found"),
    FLOW_GET_ERROR(20209, "Workflow retrieval failed"),
    FLOW_EXECUTE_ERROR(20209, "Workflow execute failed"),

    // OpenAPI errors
    OPEN_API_ERROR(20805, "OpenAPI output error");

    private final int code;
    private final String msg;
    
    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}