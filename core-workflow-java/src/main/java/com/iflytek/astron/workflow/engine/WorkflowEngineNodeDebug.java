package com.iflytek.astron.workflow.engine;

import com.alibaba.fastjson2.JSON;
import com.iflytek.astron.workflow.controller.vo.NodeDebugRespVo;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.WorkflowDSL;
import com.iflytek.astron.workflow.engine.domain.callbacks.GenerateUsage;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.node.NodeExecutor;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import com.iflytek.astron.workflow.engine.node.callback.WorkflowMsgCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node debugging service
 * Handles debugging of individual workflow nodes in isolation
 *
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Slf4j
@Service
public class WorkflowEngineNodeDebug {

    private final WorkflowEngine workflowEngine;
    private final Map<NodeTypeEnum, NodeExecutor> nodeExecutorsMap;

    public WorkflowEngineNodeDebug(WorkflowEngine workflowEngine, List<NodeExecutor> nodeExecutors) {
        this.workflowEngine = workflowEngine;
        // Build the map from the list
        this.nodeExecutorsMap = new HashMap<>();
        for (NodeExecutor executor : nodeExecutors) {
            this.nodeExecutorsMap.put(executor.getNodeType(), executor);
        }
    }

    /**
     * Debug a single node in isolation
     *
     * @param workflowDSL The workflow DSL containing node definition and configuration
     * @param flowId      The flow ID for the debug operation
     * @return Node debug response containing execution results and metrics
     * @throws Exception if debugging fails
     */
    public NodeDebugRespVo nodeDebug(WorkflowDSL workflowDSL, String flowId) throws Exception {

        long timeStart = System.currentTimeMillis();
        log.info("Starting node debug: flowId={}, nodeCount={}", flowId, workflowDSL.getNodes().size());

        // Initialize variable pool with the node protocol
        VariablePool variablePool = new VariablePool();

        // Get the first node (the node to debug)
        if (workflowDSL.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow DSL contains no nodes");
        }

        Node nodeToDebug = workflowDSL.getNodes().get(0);
        nodeToDebug.init();
        log.info("Debugging node: id={}, type={}, alias={}",
                nodeToDebug.getId(), nodeToDebug.getNodeType(), nodeToDebug.getData().getNodeMeta().getAliasName());

        // Get the appropriate executor for this node type
        NodeExecutor executor = nodeExecutorsMap.get(nodeToDebug.getNodeType());
        if (executor == null) {
            throw new IllegalStateException("No executor found for node type: " + nodeToDebug.getNodeType());
        }


        // Create a dummy callback for debugging (no streaming needed)
        StreamCallback dummyCallback = new StreamCallback() {
            @Override
            public void callback(String eventType, Object data) {
                // Do nothing for debugging
                log.debug("Debug callback: {} -> {}", eventType, data);
            }

            @Override
            public void finished() {
                // Do nothing for debugging
                log.debug("Debug callback finished");
            }
        };

        // Create node state for the execution
        WorkflowMsgCallback workflowCallback = new WorkflowMsgCallback(
                "debug-sid-" + System.currentTimeMillis(),
                dummyCallback,
                null,
                new java.util.concurrent.ConcurrentLinkedQueue<>(),
                new java.util.concurrent.ConcurrentLinkedQueue<>()
        );

        // init context
        EngineContextHolder.initContext(workflowDSL.getFlowId(), workflowDSL.getUuid(), workflowCallback);

        try {
            // Execute the node
            NodeRunResult result = executor.execute(new com.iflytek.astron.workflow.engine.domain.NodeState(
                    nodeToDebug, variablePool, workflowCallback));

            // Calculate execution time
            double timeCost = (System.currentTimeMillis() - timeStart) / 1000.0;

            // Check execution status and throw exception if failed
            if (!result.getStatus().isSuccess() && result.getError() != null) {
                throw result.getError();
            }

            // Build response object with execution results
            NodeDebugRespVo nodeDebugRespVo = new NodeDebugRespVo();
            nodeDebugRespVo.setNodeId(nodeToDebug.getId());
            nodeDebugRespVo.setAliasName(nodeToDebug.getData().getNodeMeta().getAliasName());
            nodeDebugRespVo.setNodeType(nodeToDebug.getNodeType().getValue());
            nodeDebugRespVo.setInput(JSON.toJSONString(result.getInputs()));
            nodeDebugRespVo.setRawOutput(result.getRawOutput() != null ? result.getRawOutput() : "");
            nodeDebugRespVo.setOutput(JSON.toJSONString(result.getOutputs()));
            nodeDebugRespVo.setNodeExecCost(String.format("%.3f", timeCost));
            nodeDebugRespVo.setTokenCost(result.getTokenCost() != null ? result.getTokenCost() : new GenerateUsage());

            log.info("Node debug completed: nodeId={}, execTime={}s", nodeToDebug.getId(), timeCost);
            return nodeDebugRespVo;
        } finally {
            EngineContextHolder.remove();
        }
    }
}