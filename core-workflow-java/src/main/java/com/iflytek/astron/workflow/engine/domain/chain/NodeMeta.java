package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Node metadata.
 * 
 * This class represents the metadata associated with a single node in a workflow,
 * including the node type and human-readable alias name.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeMeta {
    
    /**
     * Type of the node (e.g., "node-start", "node-llm", "node-plugin", "node-end")
     */
    @JsonProperty("nodeType")
    private String nodeType;
    
    /**
     * Human-readable alias name
     */
    @JsonProperty("aliasName")
    private String aliasName;
}
