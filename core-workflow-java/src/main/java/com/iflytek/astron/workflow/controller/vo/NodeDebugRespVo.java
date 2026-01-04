package com.iflytek.astron.workflow.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iflytek.astron.workflow.engine.domain.callbacks.GenerateUsage;
import lombok.Data;

/**
 * Response value object for node debugging.
 * 
 * This class represents the response structure for node debugging operations,
 * containing execution results and metrics similar to the Python implementation.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
public class NodeDebugRespVo {

    /**
     * Node identifier
     */
    @JsonProperty("node_id")
    private String nodeId;

    /**
     * Node alias name
     */
    @JsonProperty("alias_name")
    private String aliasName;

    /**
     * Type of the node
     */
    @JsonProperty("node_type")
    private String nodeType;

    /**
     * Input data as JSON string
     */
    private String input;

    /**
     * Raw output data as string
     */
    @JsonProperty("raw_output")
    private String rawOutput;

    /**
     * Processed output data as JSON string
     */
    private String output;

    /**
     * Node execution cost in seconds as string
     */
    @JsonProperty("node_exec_cost")
    private String nodeExecCost;

    /**
     * Token usage cost information
     */
    @JsonProperty("token_cost")
    private GenerateUsage tokenCost;

    public NodeDebugRespVo() {
        this.tokenCost = new GenerateUsage();
    }

    /**
     * Constructor with all parameters
     */
    public NodeDebugRespVo(String nodeId, String aliasName, String nodeType, 
                          String input, String rawOutput, String output, 
                          String nodeExecCost, GenerateUsage tokenCost) {
        this.nodeId = nodeId;
        this.aliasName = aliasName;
        this.nodeType = nodeType;
        this.input = input;
        this.rawOutput = rawOutput;
        this.output = output;
        this.nodeExecCost = nodeExecCost;
        this.tokenCost = tokenCost != null ? tokenCost : new GenerateUsage();
    }
}