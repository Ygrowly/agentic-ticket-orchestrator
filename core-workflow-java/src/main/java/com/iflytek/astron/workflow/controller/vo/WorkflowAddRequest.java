package com.iflytek.astron.workflow.controller.vo;

import lombok.Data;

import java.util.Map;

/**
 * Workflow add request
 */
@Data
public class WorkflowAddRequest {
    private Long groupId;
    private String name;
    private Map<String, Object> data;
    private String description;
    private String appId;
    private Integer source;
    private String version;
    private Integer tag;
}