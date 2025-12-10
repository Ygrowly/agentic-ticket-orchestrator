package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Output item with name and schema.
 * 
 * This class represents a single output item of a node in a workflow,
 * including the output name, schema definition, and whether it is required.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputItem {
    
    @JsonProperty("id")
    private String id;
    
    /**
     * Output name (e.g., "llm_output", "voice_url")
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * Output schema definition
     */
    @JsonProperty("schema")
    private Map<String, Object> schema;
    
    /**
     * Whether this output is required
     */
    @JsonProperty("required")
    private boolean required;
}
