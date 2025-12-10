package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input item with name and schema.
 * 
 * This class represents a single input parameter in a workflow,
 * with a unique identifier, name, and schema definition.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputItem {
    
    @JsonProperty("id")
    private String id;
    
    /**
     * Input name (e.g., "user_input", "text")
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * Input schema definition
     */
    @JsonProperty("schema")
    private InputSchema schema;
}
