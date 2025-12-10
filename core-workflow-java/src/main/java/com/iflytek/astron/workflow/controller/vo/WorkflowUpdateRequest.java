package com.iflytek.astron.workflow.controller.vo;

import lombok.Data;

import java.util.Map;

/**
 * Workflow update request
 */
@Data
public class WorkflowUpdateRequest {
    private String name;
    private String description;
    private Map<String, Object> data;
    private String appId;
}