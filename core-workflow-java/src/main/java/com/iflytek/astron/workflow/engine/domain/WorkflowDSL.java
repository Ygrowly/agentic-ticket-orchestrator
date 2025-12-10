package com.iflytek.astron.workflow.engine.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iflytek.astron.workflow.engine.domain.chain.Edge;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow DSL (Domain Specific Language) structure.
 * Represents the complete workflow definition with nodes and edges.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDSL {
    /**
     * Workflow ID
     */
    private String flowId;

    /**
     * Workflow UUID
     */
    private String uuid;

    /**
     * List of nodes in the workflow
     */
    @JsonProperty("nodes")
    private List<Node> nodes = new ArrayList<>();

    /**
     * List of edges connecting nodes
     */
    @JsonProperty("edges")
    private List<Edge> edges = new ArrayList<>();

    /**
     * Find a node by ID
     *
     * @param nodeId node ID to search for
     * @return the node if found, null otherwise
     */
    public Node findNodeById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all edges from a specific source node
     *
     * @param sourceNodeId source node ID
     * @return list of outgoing edges
     */
    public List<Edge> getOutgoingEdges(String sourceNodeId) {
        List<Edge> outgoing = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getSource().equals(sourceNodeId)) {
                outgoing.add(edge);
            }
        }
        return outgoing;
    }
}
