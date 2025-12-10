package com.iflytek.astron.workflow.engine.domain.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iflytek.astron.workflow.engine.constants.NodeStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Workflow node with ID and data.
 * 
 * This class represents a single node in a workflow,
 * with a unique identifier, data configuration, and execution status.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /**
     * Node ID in format: "node-type::sequenceId"
     * Examples: "node-start::001", "node-llm::002", "node-plugin::003", "node-end::004"
     */
    @JsonProperty("id")
    private String id;

    /**
     * Node data containing configuration and parameters
     */
    @JsonProperty("data")
    private NodeData data;

    private NodeStatusEnum status;

    /**
     * 前置Node，前面的这些node都执行完毕之后，才会执行当前node
     */
    private List<Node> preNodes;

    /**
     * 后置Node，当前node执行成功之后，执行后续的node
     */
    private List<Node> nextNodes;

    /**
     * 失败Node，当前node执行失败之后，执行后续的node
     */
    private List<Node> failNodes;

    /**
     * 当前node已经执行了多少次
     */
    private AtomicInteger executedCount;

    /**
     * Extract node type from ID
     *
     * @return node type (e.g., "node-start", "node-llm", "node-plugin", "node-end")
     */
    public NodeTypeEnum getNodeType() {
        if (id != null && id.contains("::")) {
            return NodeTypeEnum.fromValue(id.split("::")[0]);
        }
        return null;
    }

    public void init() {
        status = NodeStatusEnum.INIT;
        preNodes = new ArrayList<>();
        nextNodes = new ArrayList<>();
        failNodes = new ArrayList<>();
        executedCount = new AtomicInteger(0);
    }
}
