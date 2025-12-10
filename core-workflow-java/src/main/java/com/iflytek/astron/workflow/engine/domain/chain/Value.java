package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a value that can be either a literal or a reference to another node.
 * 
 * This class is used to represent the input values of a node in a workflow,
 * which can either be a direct literal value or a reference to an output variable of another node.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Value {
    
    /**
     * Type of the value: "ref" or "literal"
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * Content can be NodeRef (for type="ref") or primitive value (for type="literal")
     */
    @JsonProperty("content")
    private Object content;
    
    public boolean isReference() {
        return "ref".equals(type);
    }
    
    public NodeRef getNodeRef() {
        if (isReference() && content instanceof NodeRef) {
            return (NodeRef) content;
        }
        return null;
    }
}
