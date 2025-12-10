package com.iflytek.astron.workflow.engine.domain.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Main response structure for LLM generation results.
 *
 * This class represents the complete response structure for workflow execution,
 * compatible with OpenAI's streaming API format.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
public class LLMGenerate {
    /**
     * Status code returned by the model or workflow engine.
     */
    private int code = 0;

    /**
     * Status message describing the result.
     */
    private String message = "Success";

    /**
     * Session identifier (sid) for tracking the request.
     */
    private String id;

    /**
     * The Unix timestamp (in seconds) of when the chat completion was created.
     */
    private long created;

    /**
     * Workflow execution step information.
     * This field is specific to workflow execution and not part of OpenAI's standard format.
     */
    @JsonProperty("workflow_step")
    private WorkflowStep workflowStep = new WorkflowStep();

    /**
     * List of response choices containing delta content.
     */
    private List<Choice> choices = new ArrayList<>();

    /**
     * Usage statistics for the completion request.
     */
    private GenerateUsage usage = null;

    /**
     * Interrupt event data if the workflow was interrupted.
     */
    @JsonProperty("event_data")
    private InterruptData eventData = null;

    public LLMGenerate() {
        this.created = System.currentTimeMillis() / 1000;
    }

    public LLMGenerate(int code, String message, String id, long created,
                      WorkflowStep workflowStep, List<Choice> choices,
                      GenerateUsage usage, InterruptData eventData) {
        this.code = code;
        this.message = message;
        this.id = id;
        this.created = created;
        this.workflowStep = workflowStep;
        this.choices = choices;
        this.usage = usage;
        this.eventData = eventData;
    }

    /**
     * Build a common response result.
     *
     * @param sid Session or request unique identifier for tracking
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @param workflowUsage Workflow execution usage statistics, such as token consumption
     * @param nodeInfo Current node metadata information (e.g., node ID, type, etc.)
     * @param progress Execution progress, typically in range [0,1], default 1 indicates completion
     * @param content Main output content of the node or response, default empty string
     * @param reasoningContent Reasoning process or intermediate output for enhanced explainability, default empty string
     * @param finishReason Completion reason (e.g., "stop", "length", etc.), default null
     * @return LLMGenerate instance with the specified parameters
     */
    public static LLMGenerate common(String sid, int code, String message,
                                   GenerateUsage workflowUsage, NodeInfo nodeInfo,
                                   double progress, String content,
                                   String reasoningContent, String finishReason) {
        WorkflowStep workflowStep = new WorkflowStep(nodeInfo, 0, progress);

        Delta delta = new Delta("assistant", content, reasoningContent);
        Choice choice = new Choice(delta, 0, finishReason);

        List<Choice> choices = new ArrayList<>();
        choices.add(choice);

        LLMGenerate resp = new LLMGenerate();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setId(sid);
        resp.setCreated(System.currentTimeMillis() / 1000);
        resp.setWorkflowStep(workflowStep);
        resp.setChoices(choices);

        if (workflowUsage != null) {
            resp.setUsage(workflowUsage);
        }

        return resp;
    }

    /**
     * Build interrupt event response result.
     *
     * @param sid Session or request unique identifier for tracking
     * @param eventData Interrupt event related data structure containing context information
     * @param code Status code, default 0 indicates normal
     * @param message Status message description, default "Success"
     * @param nodeInfo Information about the node that triggered the interrupt
     * @param progress Current execution progress, typically in range [0,1]
     * @param finishReason Interrupt/completion reason
     * @return LLMGenerate instance for the interrupt event
     */
    public static LLMGenerate interrupt(String sid, InterruptData eventData, int code,
                                     String message, NodeInfo nodeInfo, double progress,
                                     String finishReason) {
        WorkflowStep workflowStep = new WorkflowStep(nodeInfo, 0, progress);

        Delta delta = new Delta("assistant", "", "");
        Choice choice = new Choice(delta, 0, finishReason);

        List<Choice> choices = new ArrayList<>();
        choices.add(choice);

        LLMGenerate resp = new LLMGenerate();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setId(sid);
        resp.setCreated(System.currentTimeMillis() / 1000);
        resp.setWorkflowStep(workflowStep);
        resp.setChoices(choices);
        resp.setEventData(eventData);

        return resp;
    }

    /**
     * Build ping response result.
     *
     * @param sid Session or request unique identifier for tracking
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @param nodeInfo Node information
     * @param progress Current execution progress
     * @return LLMGenerate instance for ping event
     */
    public static LLMGenerate ping(String sid, int code, String message,
                                 NodeInfo nodeInfo, double progress) {
        WorkflowStep workflowStep = new WorkflowStep(nodeInfo, 0, progress);

        Delta delta = new Delta("assistant", "", "");
        Choice choice = new Choice(delta, 0, "ping");

        List<Choice> choices = new ArrayList<>();
        choices.add(choice);

        LLMGenerate resp = new LLMGenerate();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setId(sid);
        resp.setCreated(System.currentTimeMillis() / 1000);
        resp.setWorkflowStep(workflowStep);
        resp.setChoices(choices);

        return resp;
    }

    /**
     * Build workflow start event response result.
     *
     * @param sid Session or request unique identifier for tracking workflow startup
     * @return LLMGenerate instance for workflow start event
     */
    public static LLMGenerate workflowStart(String sid) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId("flow_obj");
        nodeInfo.setAliasName("flow_start");
        nodeInfo.setFinishReason("stop");
        nodeInfo.setInputs(new HashMap<>());
        nodeInfo.setOutputs(new HashMap<>());
        nodeInfo.setExecutedTime(0);
        nodeInfo.setUsage(new GenerateUsage(0, 0, 0));

        return common(sid, 0, "Success", null, nodeInfo, 0, "", "", null);
    }

    /**
     * Build workflow end event response result.
     *
     * @param sid Session or request unique identifier for tracking workflow execution
     * @param workflowUsage Workflow execution usage statistics
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @return LLMGenerate instance for workflow end event
     */
    public static LLMGenerate workflowEnd(String sid, GenerateUsage workflowUsage,
                                        int code, String message) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId("flow_obj");
        nodeInfo.setAliasName("flow_end");
        nodeInfo.setFinishReason("stop");
        nodeInfo.setInputs(new HashMap<>());
        nodeInfo.setOutputs(new HashMap<>());
        nodeInfo.setExecutedTime(0);
        nodeInfo.setUsage(new GenerateUsage(0, 0, 0));

        return common(sid, code, message, workflowUsage, nodeInfo, 1, "", "", "stop");
    }

    /**
     * Build workflow abnormal end event response result.
     *
     * @param sid Session or request unique identifier for tracking workflow execution
     * @param code Error code for identifying the exception type
     * @param message Error description information explaining the specific exception cause
     * @return LLMGenerate instance for workflow error end event
     */
    public static LLMGenerate workflowEndError(String sid, int code, String message) {
        GenerateUsage usage = new GenerateUsage(0, 0, 0);
        LLMGenerate llmGenerate = workflowEnd(sid, usage, code, message);
        if (llmGenerate.getWorkflowStep() != null) {
            llmGenerate.getWorkflowStep().setNode(null);
        }
        return llmGenerate;
    }

    /**
     * Build node start event response result.
     *
     * @param sid Session or request unique identifier for tracking the workflow
     * @param nodeId Unique identifier of the node for locating specific node in workflow
     * @param aliasName Alias name of the node, typically used for frontend display
     * @param progress Current node execution progress, typically in range [0,1]
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @return LLMGenerate instance for node start event
     */
    public static LLMGenerate nodeStart(String sid, String nodeId, String aliasName,
                                      double progress, int code, String message) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setAliasName("Node:" + aliasName);
        nodeInfo.setFinishReason(null);
        nodeInfo.setInputs(new HashMap<>());
        nodeInfo.setOutputs(new HashMap<>());
        nodeInfo.setExecutedTime(0);

        return common(sid, code, message, null, nodeInfo, progress, "", "", null);
    }

    /**
     * Build node execution process event response result.
     *
     * @param sid Session or request unique identifier for tracking the workflow
     * @param nodeId Unique identifier of the node for locating specific node in workflow
     * @param aliasName Alias name of the node, typically used for display
     * @param nodeExecutedTime Time the node has been executing, in seconds
     * @param nodeExt Node extension information
     * @param progress Current node execution progress, typically in range [0,1]
     * @param content Main output result of the node
     * @param reasoningContent Reasoning process or intermediate results
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @return LLMGenerate instance for node process event
     */
    public static LLMGenerate nodeProcess(String sid, String nodeId, String aliasName,
                                        double nodeExecutedTime, Map<String, Object> nodeExt,
                                        double progress, String content, String reasoningContent,
                                        int code, String message) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setAliasName("Node:" + aliasName);
        nodeInfo.setFinishReason(null);
        nodeInfo.setInputs(new HashMap<>());
        nodeInfo.setOutputs(new HashMap<>());
        nodeInfo.setExecutedTime(nodeExecutedTime);
        nodeInfo.setExt(nodeExt);

        return common(sid, code, message, null, nodeInfo, progress, content, reasoningContent, null);
    }

    /**
     * Build node interrupt event response result.
     *
     * @param sid Session or request unique identifier for tracking the workflow
     * @param eventId Unique identifier for the interrupt event
     * @param value Specific data of the interrupt event
     * @param nodeId Unique identifier of the node for locating the interrupted node
     * @param aliasName Alias name of the node for more friendly display
     * @param nodeExecutedTime Time the node has been executing, in seconds
     * @param nodeExt Node extension information
     * @param progress Current node execution progress, typically in range [0,1]
     * @param finishReason Interrupt reason
     * @param needReply Whether to send interrupt response to frontend
     * @param code Status code, default 0 indicates success
     * @param message Status message description, default "Success"
     * @return LLMGenerate instance for node interrupt event
     */
    public static LLMGenerate nodeInterrupt(String sid, String eventId, Map<String, Object> value,
                                          String nodeId, String aliasName, double nodeExecutedTime,
                                          Map<String, Object> nodeExt, double progress,
                                          String finishReason, boolean needReply, int code,
                                          String message) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setAliasName("Node:" + aliasName);
        nodeInfo.setFinishReason(finishReason);
        nodeInfo.setInputs(new HashMap<>());
        nodeInfo.setOutputs(new HashMap<>());
        nodeInfo.setExecutedTime(nodeExecutedTime);
        nodeInfo.setExt(nodeExt);

        InterruptData eventData = new InterruptData();
        eventData.setEventId(eventId);
        eventData.setEventType("interrupt");
        eventData.setNeedReply(needReply);
        eventData.setValue(value);

        return interrupt(sid, eventData, code, message, nodeInfo, progress, finishReason);
    }

    // Getters and setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public WorkflowStep getWorkflowStep() {
        return workflowStep;
    }

    public void setWorkflowStep(WorkflowStep workflowStep) {
        this.workflowStep = workflowStep;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public GenerateUsage getUsage() {
        return usage;
    }

    public void setUsage(GenerateUsage usage) {
        this.usage = usage;
    }

    public InterruptData getEventData() {
        return eventData;
    }

    public void setEventData(InterruptData eventData) {
        this.eventData = eventData;
    }
}