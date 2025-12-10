package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Edge connecting two nodes in the workflow.
 * 
 * This class represents a directed connection between two nodes,
 * with a source node, target node, and optional source handle.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge {
    
    /**
     * Source node ID
     */
    @JsonProperty("sourceNodeId")
    private String sourceNodeId;
    
    /**
     * Target node ID
     */
    @JsonProperty("targetNodeId")
    private String targetNodeId;
    
    /**
     * Source handle (output name from source node)
     */
    @JsonProperty("sourceHandle")
    private String sourceHandle;
    
    public String getSource() {
        return sourceNodeId;
    }
    
    public String getTarget() {
        return targetNodeId;
    }
}
