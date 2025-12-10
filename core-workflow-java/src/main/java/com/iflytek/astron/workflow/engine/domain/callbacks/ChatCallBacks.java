package com.iflytek.astron.workflow.engine.domain.callbacks;

import com.iflytek.astron.workflow.engine.constants.ChatStatusEnum;
import com.iflytek.astron.workflow.engine.constants.EndNodeOutputModeEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.exception.ErrorCode;
import com.iflytek.astron.workflow.exception.NodeCustomException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main callback handler for workflow execution events.
 * <p>
 * This class manages the streaming of workflow execution events, handles node
 * lifecycle callbacks, and coordinates with various queues for ordered output.
 * It provides methods for different stages of workflow and node execution.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Slf4j
public class ChatCallBacks {
    private String sid;
    private GenerateUsage generateUsage = new GenerateUsage();
    @Getter
    private Queue<LLMGenerate> streamQueue;
    private Map<String, Long> nodeExecuteStartTime = new ConcurrentHashMap<>();
    private EndNodeOutputModeEnum endNodeOutputMode;
    private Set<String> supportStreamNodeIdSet;
    @Getter
    private Queue<ChatCallBackStreamResult> orderStreamResultQ;


    public ChatCallBacks(String sid, Queue<LLMGenerate> streamQueue,
                         EndNodeOutputModeEnum endNodeOutputMode, Set<String> supportStreamNodeIds,
                         Queue<ChatCallBackStreamResult> needOrderStreamResultQ) {
        this.sid = sid;
        this.streamQueue = streamQueue;
        this.endNodeOutputMode = endNodeOutputMode;
        this.supportStreamNodeIdSet = supportStreamNodeIds;
        this.orderStreamResultQ = needOrderStreamResultQ;
    }

    /**
     * Calculate the current execution progress of the workflow.
     * <p>
     * Progress calculation rules:
     * - Simplified progress calculation without chains dependency
     *
     * @param currentExecuteNodeId ID of the currently executing node
     * @return Progress value between 0.0 and 1.0
     */
    private double getNodeProgress(String currentExecuteNodeId) {
        // 简化实现，不依赖Chains
        return 0.0;
    }

    /**
     * Handle workflow start event.
     * <p>
     * Creates and queues a workflow start response, then handles event stream interruption.
     */
    public void onSparkflowStart() {
        LLMGenerate resp = LLMGenerate.workflowStart(this.sid);
        this.putFrameIntoQueue("WorkflowStart", resp, null);
    }

    /**
     * Handle workflow end event.
     * <p>
     * Creates and queues a workflow end response with usage statistics and error information.
     *
     * @param message Final node run result containing execution summary
     */
    public void onSparkflowEnd(NodeRunResult message) {
        int code = ErrorCode.Success.getCode();
        String msg = ErrorCode.Success.getMsg();

        if (message.getError() != null) {
            code = message.getError().getCode();
            msg = message.getError().getMessage();
        }

        LLMGenerate resp = LLMGenerate.workflowEnd(
                this.sid,
                this.generateUsage,
                code,
                msg
        );
        this.putFrameIntoQueue("WorkflowEnd", resp, null);
    }

    /**
     * Handle node start event.
     * <p>
     * Records the start time and creates a node start response with progress information.
     *
     * @param code      Status code for the node start operation
     * @param nodeId    Unique identifier of the starting node
     * @param aliasName Human-readable name for the node
     */
    public void onNodeStart(int code, String nodeId, String aliasName) {
        this.nodeExecuteStartTime.put(nodeId, System.currentTimeMillis());
        LLMGenerate resp = LLMGenerate.nodeStart(
                this.sid,
                nodeId,
                aliasName,
                this.getNodeProgress(nodeId),
                code,
                "Success"
        );
        this.putFrameIntoQueue(nodeId, resp);
    }

    /**
     * Handle node processing event.
     * <p>
     * Creates a node process response with execution time, progress, and content.
     * Handles special cases for end nodes and error conditions.
     *
     * @param code             Status code for the node processing operation
     * @param nodeId           Unique identifier of the processing node
     * @param aliasName        Human-readable name for the node
     * @param message          Processing message or error content
     * @param reasoningContent Additional reasoning or intermediate content
     */
    public void onNodeProcess(int code, String nodeId, String aliasName,
                              String message, String reasoningContent) {
        Map<String, Object> ext = null;
        if (nodeId.split(":")[0].equals(NodeTypeEnum.END.getValue())) {
            ext = new HashMap<>();
            ext.put("answer_mode", this.endNodeOutputMode.getValue());
        }

        String content = (code == 0) ? message : "";  // If error occurs, content is empty
        if (nodeId.split(":")[0].equals(NodeTypeEnum.END.getValue())) {
            if (this.endNodeOutputMode == EndNodeOutputModeEnum.VARIABLE_MODE) {
                content = "";
            }
        }

        long startTime = this.nodeExecuteStartTime.getOrDefault(nodeId, System.currentTimeMillis());
        double executedTime = (System.currentTimeMillis() - startTime) / 1000.0;

        LLMGenerate resp = LLMGenerate.nodeProcess(
                this.sid,
                nodeId,
                aliasName,
                executedTime,
                ext,
                this.getNodeProgress(nodeId),
                content,
                reasoningContent,
                code,
                (code == 0) ? "Success" : message
        );
        this.putFrameIntoQueue(nodeId, resp);
    }

    /**
     * Handle node interrupt event.
     * <p>
     * Creates an interrupt response and sets the resume event flag for event stream handling.
     *
     * @param eventId      Unique identifier for the interrupt event
     * @param value        Interrupt event data
     * @param nodeId       Unique identifier of the interrupted node
     * @param aliasName    Human-readable name for the node
     * @param code         Status code for the interrupt operation
     * @param finishReason Reason for the interrupt
     * @param needReply    Whether a reply is needed for the interrupt
     */
    public void onNodeInterrupt(String eventId, Map<String, Object> value,
                                String nodeId, String aliasName, int code,
                                String finishReason, boolean needReply) {
        long startTime = this.nodeExecuteStartTime.getOrDefault(nodeId, System.currentTimeMillis());
        double executedTime = (System.currentTimeMillis() - startTime) / 1000.0;

        LLMGenerate resp = LLMGenerate.nodeInterrupt(
                this.sid,
                eventId,
                value,
                nodeId,
                aliasName,
                executedTime,
                null,
                this.getNodeProgress(nodeId),
                finishReason,
                needReply,
                code,
                "Success"
        );
        this.putFrameIntoQueue(nodeId, resp);
    }

    /**
     * Handle node end event.
     * <p>
     * Processes the final result of a node execution, handling both success and error cases.
     * Updates usage statistics and creates appropriate response based on node type.
     *
     * @param nodeId    Unique identifier of the completed node
     * @param aliasName Human-readable name for the node
     * @param message   Node execution result, null if execution failed
     * @param error     Exception if node execution failed, null if successful
     */
    public void onNodeEnd(String nodeId, String aliasName,
                          NodeRunResult message, NodeCustomException error) {
        String nodeType = nodeId.split(":")[0];
        Map<String, Object> ext = new HashMap<>();

        if (error != null) {
            this.onNodeEndError(nodeId, aliasName, error);
            return;
        }

        if (message == null) {
            NodeCustomException defaultError = new NodeCustomException(
                    ErrorCode.NODE_RUN_ERROR,
                    "Node run error, please check the node configuration"
            );
            this.onNodeEndError(nodeId, aliasName, defaultError);
            return;
        }

        if (message.getError() != null) {
            this.onNodeEndError(nodeId, aliasName, message.getError());
            return;
        }

        if (message.getTokenCost() != null) {
            this.generateUsage.add(message.getTokenCost());
        }

        if (nodeType.equals(NodeTypeEnum.LLM.getValue()) ||
                nodeType.equals(NodeTypeEnum.DECISION_MAKING.getValue())) {
            if (message.getRawOutput() != null && !message.getRawOutput().isEmpty()) {
                ext.put("raw_output", message.getRawOutput());
            }
            if (nodeType.equals(NodeTypeEnum.END.getValue())) {
                ext.put("answer_mode", this.endNodeOutputMode.getValue());
            }
        }

        String content = message.getNodeAnswerContent();
        if (nodeType.equals(NodeTypeEnum.END.getValue()) &&
                this.endNodeOutputMode == EndNodeOutputModeEnum.DIRECT_MODE) {
            // In Java, we would need to convert the outputs map to JSON string
            // This is a simplified representation
            content = message.getOutputs().toString();
        }

        long startTime = this.nodeExecuteStartTime.getOrDefault(nodeId, System.currentTimeMillis());
        double executedTime = (System.currentTimeMillis() - startTime) / 1000.0;

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setAliasName("Node:" + aliasName);
        nodeInfo.setFinishReason(ChatStatusEnum.STOP.getStatus());
        nodeInfo.setInputs(message.getInputs());
        nodeInfo.setOutputs(message.getOutputs());
        nodeInfo.setErrorOutputs(message.getErrorOutputs());
        nodeInfo.setExt(ext);
        nodeInfo.setExecutedTime(executedTime);
        nodeInfo.setUsage(message.getTokenCost());

        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setNode(nodeInfo);
        workflowStep.setProgress(this.getNodeProgress(nodeId));

        Delta delta = new Delta();
        delta.setContent(content);
        delta.setReasoningContent(message.getNodeAnswerReasoningContent());

        Choice choice = new Choice();
        choice.setDelta(delta);

        LLMGenerate resp = new LLMGenerate();
        resp.setId(this.sid);
        resp.setWorkflowStep(workflowStep);
        resp.getChoices().add(choice);

        this.putFrameIntoQueue(nodeId, resp, ChatStatusEnum.STOP.getStatus());
    }

    /**
     * Handle node end error event.
     * <p>
     * Creates an error response for a node that failed to complete successfully.
     *
     * @param nodeId    Unique identifier of the failed node
     * @param aliasName Human-readable name for the node
     * @param error     Exception containing error details
     */
    private void onNodeEndError(String nodeId, String aliasName, NodeCustomException error) {
        String nodeType = nodeId.split(":")[0];

        long startTime = this.nodeExecuteStartTime.getOrDefault(nodeId, System.currentTimeMillis());
        double executedTime = (System.currentTimeMillis() - startTime) / 1000.0;

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setAliasName(aliasName);
        nodeInfo.setFinishReason("stop");
        nodeInfo.setExecutedTime(executedTime);

        if (nodeType.equals(NodeTypeEnum.LLM.getValue()) ||
                nodeType.equals(NodeTypeEnum.DECISION_MAKING.getValue())) {
            nodeInfo.setUsage(new GenerateUsage());
        }

        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setNode(nodeInfo);
        workflowStep.setProgress(this.getNodeProgress(nodeId));

        Delta delta = new Delta();

        Choice choice = new Choice();
        choice.setDelta(delta);

        LLMGenerate resp = new LLMGenerate();
        resp.setCode(error.getCode());
        resp.setMessage(error.getMessage());
        resp.setId(this.sid);
        resp.setWorkflowStep(workflowStep);
        resp.getChoices().add(choice);

        this.putFrameIntoQueue(nodeId, resp, "stop");
    }

    /**
     * Add node response frame to appropriate queue for ordering.
     * <p>
     * Routing logic:
     * - Message nodes and end nodes are added to orderStreamResultQ for sequencing
     * - Other nodes are directly added to streamQueue
     *
     * @param nodeId Unique identifier of the node
     * @param resp   Generated response from the node
     */
    private void putFrameIntoQueue(String nodeId, LLMGenerate resp) {
        putFrameIntoQueue(nodeId, resp, "");
    }

    /**
     * Add node response frame to appropriate queue for ordering.
     * <p>
     * Routing logic:
     * - Message nodes and end nodes are added to orderStreamResultQ for sequencing
     * - Other nodes are directly added to streamQueue
     *
     * @param nodeId       Unique identifier of the node
     * @param resp         Generated response from the node
     * @param finishReason Reason for node completion
     */
    private void putFrameIntoQueue(String nodeId, LLMGenerate resp, String finishReason) {
        String nodeType = nodeId.split(":")[0];
        if (false && (nodeType.equals(NodeTypeEnum.MESSAGE.getValue()) || nodeType.equals(NodeTypeEnum.END.getValue()) || "WorkflowEnd".equals(nodeId)) ) {
            ChatCallBackStreamResult result = new ChatCallBackStreamResult(nodeId, resp, finishReason);
            log.info("orderStreamResultQ nodeId: {}, finishReason: {}", nodeId, finishReason);
            this.orderStreamResultQ.offer(result);
        } else {
            log.info("putFrameIntoQueue nodeId: {}, finishReason: {}", nodeId, finishReason);
            this.streamQueue.offer(resp);
        }
    }
}