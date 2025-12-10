package com.iflytek.astron.workflow.controller.vo;

import lombok.Data;

/**
 * Delete comparison request
 */
@Data
public class DeleteComparisonRequest {
    private String flowId;
    private String version;
}