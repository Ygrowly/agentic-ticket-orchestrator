package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Node reference for variable references like {{node-llm::002.llm_output}}.
 * 
 * This class represents a reference to a specific output variable of a node in a workflow,
 * using the node ID and variable name.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeRef {
    
    /**
     * ID of the referenced node (e.g., "node-llm::002")
     */
    @JsonProperty("nodeId")
    private String nodeId;
    
    /**
     * Name of the output variable (e.g., "llm_output")
     */
    @JsonProperty("name")
    private String name;
}
