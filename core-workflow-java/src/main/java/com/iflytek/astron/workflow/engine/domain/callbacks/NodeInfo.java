package com.iflytek.astron.workflow.engine.domain.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.HashMap;

/**
 * Information about an executed workflow node.
 * 
 * This class contains metadata about a node's execution including inputs,
 * outputs, execution time, and completion status.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
public class NodeInfo {
    /**
     * Unique identifier of the node.
     */
    private String id = "";
    
    /**
     * Human-readable name for the node.
     */
    @JsonProperty("alias_name")
    private String aliasName = "";
    
    /**
     * Reason for node completion: 'stop' for normal completion, 'interrupt' for interruption, null for ongoing.
     */
    @JsonProperty("finish_reason")
    private String finishReason = null;
    
    /**
     * Input data passed to the node.
     */
    private Map<String, Object> inputs = new HashMap<>();
    
    /**
     * Output data produced by the node.
     */
    private Map<String, Object> outputs = new HashMap<>();
    
    /**
     * Error information if the node failed.
     */
    @JsonProperty("error_outputs")
    private Map<String, Object> errorOutputs = new HashMap<>();
    
    /**
     * Additional extension data for the node.
     */
    private Map<String, Object> ext = null;
    
    /**
     * Time taken to execute the node in seconds.
     */
    @JsonProperty("executed_time")
    private double executedTime = 0.0;
    
    /**
     * Token usage information for LLM nodes.
     */
    private GenerateUsage usage = null;
    
    public NodeInfo() {
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getAliasName() {
        return aliasName;
    }
    
    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
    
    public Map<String, Object> getInputs() {
        return inputs;
    }
    
    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }
    
    public Map<String, Object> getOutputs() {
        return outputs;
    }
    
    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }
    
    public Map<String, Object> getErrorOutputs() {
        return errorOutputs;
    }
    
    public void setErrorOutputs(Map<String, Object> errorOutputs) {
        this.errorOutputs = errorOutputs;
    }
    
    public Map<String, Object> getExt() {
        return ext;
    }
    
    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
    }
    
    public double getExecutedTime() {
        return executedTime;
    }
    
    public void setExecutedTime(double executedTime) {
        this.executedTime = executedTime;
    }
    
    public GenerateUsage getUsage() {
        return usage;
    }
    
    public void setUsage(GenerateUsage usage) {
        this.usage = usage;
    }
}