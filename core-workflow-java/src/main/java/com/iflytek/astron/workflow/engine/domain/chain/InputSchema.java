package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input schema definition.
 * 
 * This class represents the schema for an input parameter,
 * including its data type and value definition.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputSchema {
    
    /**
     * Data type: "string", "boolean", "integer", "number", "array", "object"
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * Value definition (literal or reference)
     */
    @JsonProperty("value")
    private Value value;
}
