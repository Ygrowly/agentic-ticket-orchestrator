package com.iflytek.astron.workflow.controller;

import com.iflytek.astron.workflow.engine.VariablePool;
import com.iflytek.astron.workflow.engine.WorkflowEngine;
import com.iflytek.astron.workflow.engine.util.AsyncUtil;
import com.iflytek.astron.workflow.engine.domain.WorkflowDSL;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import com.iflytek.astron.workflow.engine.node.callback.SseStreamCallback;
import com.iflytek.astron.workflow.flow.service.WorkflowService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Workflow execution controller
 * Provides REST API for workflow execution with SSE streaming support
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowEngine workflowEngine;

    public WorkflowController(WorkflowService workflowService, WorkflowEngine workflowEngine) {
        this.workflowService = workflowService;
        this.workflowEngine = workflowEngine;
    }

    /**
     * Execute workflow with SSE streaming
     * <p>
     * Endpoint: POST /api/workflow/chat
     * Request body:
     * {
     * "flow_id": "184736",
     * "inputs": { "user_input": "介绍一下 Java" },
     * "chatId": "some-chat-id",
     * "regen": false
     * }
     * <p>
     * Response: SSE stream with events:
     * - node_start: Node execution started
     * - node_output: Node produced output
     * - node_end: Node execution completed
     * - workflow_complete: Workflow finished
     * - error: Error occurred
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflow(@RequestBody WorkflowRequest request) {
        log.info("Workflow execution request: flowId={}, inputs={}", request.getFlowId(), request.getInputs());

        SseEmitter emitter = new SseEmitter(600_000L);

        AsyncUtil.execute(() -> {
            try {
                WorkflowDSL workflowDSL = workflowService.getWorkflowDSL(request.getFlowId());
                workflowDSL.setUuid(request.getChatId());

                StreamCallback callback = new SseStreamCallback(emitter);

                workflowEngine.execute(workflowDSL, new VariablePool(), request.getInputs(), callback);

                emitter.complete();

            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("Workflow execution timeout");
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("SSE error: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        });

        return emitter;
    }

    /**
     * Workflow execution request
     */
    @Data
    public static class WorkflowRequest {

        @com.fasterxml.jackson.annotation.JsonProperty("flow_id")
        private String flowId;

        @com.fasterxml.jackson.annotation.JsonProperty("inputs")
        private Map<String, Object> inputs;

        @com.fasterxml.jackson.annotation.JsonProperty("chatId")
        private String chatId;

        @com.fasterxml.jackson.annotation.JsonProperty("regen")
        private Boolean regen;
    }
}