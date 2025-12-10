package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node data containing configuration and parameters.
 * 
 * This class represents the data associated with a single node in a workflow,
 * including input parameters, node metadata, and node-specific parameters.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeData {
    
    /**
     * Input items
     */
    @JsonProperty("inputs")
    private List<InputItem> inputs = new ArrayList<>();
    
    /**
     * Node metadata
     */
    @JsonProperty("nodeMeta")
    private NodeMeta nodeMeta;
    
    /**
     * Node-specific parameters (flexible structure for different node types)
     */
    @JsonProperty("nodeParam")
    private Map<String, Object> nodeParam = new HashMap<>();
    
    /**
     * Output items
     */
    @JsonProperty("outputs")
    private List<OutputItem> outputs = new ArrayList<>();

    @JsonProperty("retryConfig")
    private RetryConfig retryConfig;

}
