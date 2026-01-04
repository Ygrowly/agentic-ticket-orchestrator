package com.iflytek.astron.workflow.controller;

import com.iflytek.astron.workflow.controller.vo.NodeDebugRespVo;
import com.iflytek.astron.workflow.controller.vo.RspVo;
import com.iflytek.astron.workflow.engine.VariablePool;
import com.iflytek.astron.workflow.engine.WorkflowEngine;
import com.iflytek.astron.workflow.engine.WorkflowEngineNodeDebug;
import com.iflytek.astron.workflow.engine.domain.WorkflowDSL;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import com.iflytek.astron.workflow.engine.node.callback.SseStreamCallback;
import com.iflytek.astron.workflow.engine.util.AsyncUtil;
import com.iflytek.astron.workflow.flow.service.WorkflowService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 这里适用于流程编排调试时，直接替换python - workflow工程中的 chat/debug
 * <p>
 * Workflow execution controller
 * Provides REST API for workflow execution with SSE streaming
 *
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping({"/workflow/v1"})
public class WorkflowDebugController {

    private final WorkflowService workflowService;
    private final WorkflowEngineNodeDebug workflowEngineNodeDebug;
    private final WorkflowEngine workflowEngine;

    public WorkflowDebugController(WorkflowService workflowService, WorkflowEngineNodeDebug workflowEngineNodeDebug, WorkflowEngine workflowEngine) {
        this.workflowService = workflowService;
        this.workflowEngineNodeDebug = workflowEngineNodeDebug;
        this.workflowEngine = workflowEngine;
    }

    /**
     * Execute workflow with SSE streaming
     * <p>
     * Endpoint: POST /workflow/v1/debug/chat/completions
     * Request body:
     * {"stream":true,"debug":true,"parameters":{"AGENT_USER_INPUT":"给我说一个三国的笑话吧"},"uid":"admin","flow_id":"7399634992520073218"}
     * <p>
     * Response: SSE stream with events:
     * - node_start: Node execution started
     * - node_output: Node produced output
     * - node_end: Node execution completed
     * - workflow_complete: Workflow finished
     * - error: Error occurred
     */
    @PostMapping(value = {"/debug/chat/completions"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflow(@RequestBody WorkflowDebugRequest request) {
        log.info("Workflow execution request: flowId={}, inputs={}", request.getFlowId(), request);

        SseEmitter emitter = new SseEmitter(600_000L);

        AsyncUtil.execute(() -> {
            try {
                WorkflowDSL workflowDSL = workflowService.getWorkflowDSL(request.getFlowId());
                workflowDSL.setUuid(request.uuid);

                StreamCallback callback = new SseStreamCallback(emitter);

                workflowEngine.execute(workflowDSL, new VariablePool(), request.getParameters(), callback);
            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            } finally {
                // 判断 emitter 是否已经完结，如果没有，则主动完结
                emitter.complete();
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
    public static class WorkflowDebugRequest {

        @com.fasterxml.jackson.annotation.JsonProperty("flow_id")
        private String flowId;

        /**
         * 是否流式返回
         */
        @com.fasterxml.jackson.annotation.JsonProperty("stream")
        private Boolean stream;

        @com.fasterxml.jackson.annotation.JsonProperty("debug")
        private Boolean debug;

        @com.fasterxml.jackson.annotation.JsonProperty("uid")
        private String uuid;

        /**
         * 输入参数
         */
        @com.fasterxml.jackson.annotation.JsonProperty("parameters")
        private Map<String, Object> parameters;

    }


    /**
     * Debug a node in the workflow
     * <p>
     * Endpoint: POST /workflow/v1/node/debug
     * Request body: {"id":"7399634992520073218","name":"Test Workflow","description":"Test Description","data":{"nodes":[...],"edges":[...]}}
     * <p>
     * Response: JSON response with debug execution result
     */
    @PostMapping(value = {"/node/debug"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public RspVo<NodeDebugRespVo> nodeDebug(@RequestBody NodeDebugRequest request) {
        log.info("Node debug request: id={}", request.id());

        try {
            NodeDebugRespVo result = workflowEngineNodeDebug.nodeDebug(request.data(), request.id());
            log.info("Node debug completed successfully: nodeId={}", result.getNodeId());
            return RspVo.success(result, RspVo.generateSid());
        } catch (Exception e) {
            log.error("Node debug failed: {}", e.getMessage(), e);
            return RspVo.error(-1, e.getMessage(), RspVo.generateSid());
        }
    }


    public record NodeDebugRequest(String id, String name, String description, WorkflowDSL data) {
    }
}
