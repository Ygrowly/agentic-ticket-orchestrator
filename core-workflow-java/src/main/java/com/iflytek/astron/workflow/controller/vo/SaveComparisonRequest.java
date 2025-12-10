package com.iflytek.astron.workflow.controller.vo;

import lombok.Data;

import java.util.Map;

/**
 * Save comparison request
 */
@Data
public class SaveComparisonRequest {
    private String flowId;
    private Map<String, Object> data;
    private String version;
}