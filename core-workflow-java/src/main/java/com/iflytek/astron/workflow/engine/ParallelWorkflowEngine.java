package com.iflytek.astron.workflow.engine;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.iflytek.astron.workflow.engine.constants.EndNodeOutputModeEnum;
import com.iflytek.astron.workflow.engine.constants.NodeExecStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.domain.WorkflowDSL;
import com.iflytek.astron.workflow.engine.domain.callbacks.ChatCallBackStreamResult;
import com.iflytek.astron.workflow.engine.domain.callbacks.LLMGenerate;
import com.iflytek.astron.workflow.engine.domain.chain.Edge;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.node.NodeExecutor;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import com.iflytek.astron.workflow.engine.node.callback.WorkflowMsgCallback;
import com.iflytek.astron.workflow.engine.util.FlowUtil;
import com.iflytek.astron.workflow.exception.ErrorCode;
import com.iflytek.astron.workflow.exception.NodeCustomException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parallel Workflow execution engine
 * Executes workflow nodes in parallel where possible
 */
@Slf4j
@Component
public class ParallelWorkflowEngine {

    private final Map<NodeTypeEnum, NodeExecutor> nodeExecutors;
    private final ExecutorService executorService;

    public ParallelWorkflowEngine(List<NodeExecutor> executors) {
        this.nodeExecutors = new HashMap<>();
        for (NodeExecutor executor : executors) {
            this.nodeExecutors.put(executor.getNodeType(), executor);
        }
        // Use TTL wrapper for context propagation
        this.executorService = TtlExecutors.getTtlExecutorService(Executors.newCachedThreadPool());
        log.info("Registered {} node executors for ParallelEngine", nodeExecutors.size());
    }

    public void execute(WorkflowDSL workflowDSL, VariablePool variablePool, Map<String, Object> inputs, StreamCallback callback) throws Exception {
        log.info("Starting parallel workflow execution with {} nodes", workflowDSL.getNodes().size());

        verifyWorkflow(workflowDSL);
        variablePool.clear();

        Queue<ChatCallBackStreamResult> orderStreamResultQ = new ConcurrentLinkedQueue<>();
        Queue<LLMGenerate> streamQueue = new ConcurrentLinkedQueue<>();

        Node endNode = workflowDSL.getNodes().stream().filter(s -> s.getNodeType() == NodeTypeEnum.END).findFirst().orElseThrow();
        String sid = FlowUtil.genWorkflowId(workflowDSL.getFlowId());
        WorkflowMsgCallback workflowCallback = new WorkflowMsgCallback(
                sid,
                callback,
                Objects.equals(endNode.getData().getNodeParam().get("outputMode"), 1) ? EndNodeOutputModeEnum.VARIABLE_MODE : EndNodeOutputModeEnum.DIRECT_MODE,
                streamQueue,
                orderStreamResultQ
        );

        EngineContextHolder.initContext(workflowDSL.getFlowId(), workflowDSL.getUuid(), workflowCallback);
        workflowCallback.onWorkflowStart();

        CompletableFuture<Void> workflowFuture = new CompletableFuture<>();
        AtomicInteger activeTasks = new AtomicInteger(0);

        try {
            Node startNode = buildNodeExecuteChain(workflowDSL);
            initializeStartNodeInputs(startNode, variablePool, inputs);

            // Initial task
            activeTasks.incrementAndGet();
            executorService.submit(TtlRunnable.get(() -> 
                executeNode(startNode, variablePool, workflowCallback, activeTasks, workflowFuture)
            ));

            // Wait for completion
            workflowFuture.get();
            
            log.info("Parallel Workflow: {} execution completed successfully", sid);
            workflowCallback.onWorkflowEnd(new NodeRunResult());
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            workflowCallback.onWorkflowEnd(new NodeRunResult());
            throw e;
        } finally {
            workflowCallback.finished();
            EngineContextHolder.remove();
        }
    }

    private void executeNode(Node node, VariablePool variablePool, WorkflowMsgCallback callback, AtomicInteger activeTasks, CompletableFuture<Void> workflowFuture) {
        try {
            boolean shouldRun = false;
            boolean isSkip = false;

            synchronized (node) {
                // If already running or executed, skip
                if (node.getStatus().executed() || node.getStatus() == NodeStatusEnum.RUNNING) {
                    return;
                }

                // Check all pre-nodes
                if (!CollectionUtils.isEmpty(node.getPreNodes())) {
                    for (Node preNode : node.getPreNodes()) {
                        if (!preNode.getStatus().executed()) {
                            // Pre-node not ready, abort. Current node will be triggered by pre-node later.
                            return;
                        }
                    }
                }

                // Determine execution status (MARK logic)
                if (node.getStatus() == NodeStatusEnum.MARK) {
                    boolean canExecute = false;
                    for (Node preNode : node.getPreNodes()) {
                        if (preNode.getStatus() == NodeStatusEnum.SKIP) {
                            continue;
                        } else if (preNode.getStatus() == NodeStatusEnum.ERROR) {
                            if (preNode.getFailNodes().contains(node)) {
                                canExecute = true;
                                break;
                            }
                        } else if (preNode.getStatus() == NodeStatusEnum.SUCCESS) {
                            if (preNode.getNextNodes().contains(node)) {
                                canExecute = true;
                                break;
                            }
                        }
                    }
                    if (!canExecute) {
                        node.setStatus(NodeStatusEnum.SKIP);
                        isSkip = true;
                    } else {
                        node.setStatus(NodeStatusEnum.RUNNING);
                        shouldRun = true;
                    }
                } else {
                    node.setStatus(NodeStatusEnum.RUNNING);
                    shouldRun = true;
                }
            }

            if (isSkip) {
                // Propagate skip to next nodes
                executeNormalCondition(node, variablePool, callback, activeTasks, workflowFuture);
                return;
            }

            if (shouldRun) {
                if (log.isDebugEnabled()) {
                    log.debug("Executing node in parallel: {}", node.getId());
                }
                
                NodeTypeEnum nodeType = node.getNodeType();
                NodeExecutor executor = nodeExecutors.get(nodeType);
                if (executor == null) {
                    throw new IllegalStateException("No executor found for node type: " + nodeType);
                }

                NodeExecStatusEnum execStatus;
                while (true) {
                    NodeRunResult res = executor.execute(new NodeState(node, variablePool, callback));
                    execStatus = res.getStatus();
                    if (execStatus != NodeExecStatusEnum.ERR_RETRY) {
                        break;
                    }
                }

                if (execStatus == NodeExecStatusEnum.ERR_INTERUPT) {
                    node.setStatus(NodeStatusEnum.ERROR);
                    throw new NodeCustomException(ErrorCode.INTERRUPTED_ERROR);
                } else if (execStatus == NodeExecStatusEnum.ERR_FAIL_CONDITION) {
                    node.setStatus(NodeStatusEnum.ERROR);
                    executeFailedCondition(node, variablePool, callback, activeTasks, workflowFuture);
                } else if (execStatus == NodeExecStatusEnum.ERR_CODE_MSG) {
                    node.setStatus(NodeStatusEnum.ERROR);
                    executeNormalCondition(node, variablePool, callback, activeTasks, workflowFuture);
                } else {
                    node.setStatus(NodeStatusEnum.SUCCESS);
                    executeNormalCondition(node, variablePool, callback, activeTasks, workflowFuture);
                }
            }
        } catch (Exception e) {
            workflowFuture.completeExceptionally(e);
        } finally {
            if (activeTasks.decrementAndGet() == 0) {
                workflowFuture.complete(null);
            }
        }
    }

    private void executeNormalCondition(Node node, VariablePool variablePool, WorkflowMsgCallback callback, AtomicInteger activeTasks, CompletableFuture<Void> workflowFuture) {
        // Mark fail nodes as MARK
        for (Node failNode : node.getFailNodes()) {
            synchronized (failNode) {
                if (!failNode.getStatus().executed()) {
                    failNode.setStatus(NodeStatusEnum.MARK);
                }
            }
        }
        
        // Trigger next nodes
        triggerNextNodes(node.getNextNodes(), variablePool, callback, activeTasks, workflowFuture);
    }

    private void executeFailedCondition(Node node, VariablePool variablePool, WorkflowMsgCallback callback, AtomicInteger activeTasks, CompletableFuture<Void> workflowFuture) {
        // Mark next nodes as MARK
        for (Node nextNode : node.getNextNodes()) {
            synchronized (nextNode) {
                if (!nextNode.getStatus().executed()) {
                    nextNode.setStatus(NodeStatusEnum.MARK);
                }
            }
        }

        // Trigger fail nodes
        triggerNextNodes(node.getFailNodes(), variablePool, callback, activeTasks, workflowFuture);
    }

    private void triggerNextNodes(List<Node> nextNodes, VariablePool variablePool, WorkflowMsgCallback callback, AtomicInteger activeTasks, CompletableFuture<Void> workflowFuture) {
        for (Node nextNode : nextNodes) {
            activeTasks.incrementAndGet();
            executorService.submit(TtlRunnable.get(() -> 
                executeNode(nextNode, variablePool, callback, activeTasks, workflowFuture)
            ));
        }
    }

    // Helper methods (copied from WorkflowEngine)
    private void initializeStartNodeInputs(Node startNode, VariablePool variablePool, Map<String, Object> inputs) {
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            variablePool.set(startNode.getId(), entry.getKey(), entry.getValue());
        }
    }

    private void verifyWorkflow(WorkflowDSL workflowDSL) {
        if (CollectionUtils.isEmpty(workflowDSL.getNodes()) || CollectionUtils.isEmpty(workflowDSL.getEdges())) {
            throw new IllegalStateException("Invalid workflow DSL: missing start or end node");
        }
        for (Node node : workflowDSL.getNodes()) {
            if (node.getNodeType() == null || nodeExecutors.get(node.getNodeType()) == null) {
                throw new IllegalStateException("Invalid workflow DSL: executor not found");
            }
        }

        // 环路检测
        if (hasCycle(workflowDSL)) {
            throw new IllegalStateException("Invalid workflow DSL: cycle detected");
        }
    }

    /**
     * Check for cycles in the workflow graph using Kahn's algorithm
     */
    private boolean hasCycle(WorkflowDSL workflowDSL) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        // Initialize in-degrees and adjacency list
        for (Node node : workflowDSL.getNodes()) {
            inDegree.put(node.getId(), 0);
            adjList.put(node.getId(), new ArrayList<>());
        }

        // Build graph
        for (Edge edge : workflowDSL.getEdges()) {
            String u = edge.getSource();
            String v = edge.getTarget();
            // Ensure target node exists in map (defensive programming)
            if (adjList.containsKey(u) && inDegree.containsKey(v)) {
                adjList.get(u).add(v);
                inDegree.put(v, inDegree.getOrDefault(v, 0) + 1);
            }
        }

        // Add nodes with 0 in-degree to queue
        Queue<String> queue = new java.util.LinkedList<>();
        for (String nodeId : inDegree.keySet()) {
            if (inDegree.get(nodeId) == 0) {
                queue.offer(nodeId);
            }
        }

        int processedCount = 0;
        while (!queue.isEmpty()) {
            String u = queue.poll();
            processedCount++;

            for (String v : adjList.get(u)) {
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.offer(v);
                }
            }
        }

        // If processed count != total nodes, there is a cycle
        return processedCount != workflowDSL.getNodes().size();
    }

    public Node buildNodeExecuteChain(WorkflowDSL workflowDSL) {
        try {
            Node startNode = null;
            Map<String, Node> nodeMap = new HashMap<>();
            for (Node node : workflowDSL.getNodes()) {
                if (node.getNodeType() == NodeTypeEnum.START) {
                    startNode = node;
                }
                node.init();
                nodeMap.put(node.getId(), node);
            }

            if (startNode == null) {
                throw new IllegalStateException("No start node found in workflow");
            }

            for (Edge edge : workflowDSL.getEdges()) {
                String sourceNodeId = edge.getSource();
                String targetNodeId = edge.getTarget();
                Node sourceNode = nodeMap.get(sourceNodeId);
                Node targetNode = nodeMap.get(targetNodeId);
                
                if (targetNode.getPreNodes() == null) {
                    targetNode.setPreNodes(new ArrayList<>());
                }
                targetNode.getPreNodes().add(sourceNode);

                String handle = edge.getSourceHandle();
                if (StringUtils.isNotBlank(handle)) {
                    if (handle.startsWith(NodeTypeEnum.CONDITION_SWITCH_NORMAL_ONE_OF.getValue())) {
                        sourceNode.getNextNodes().add(targetNode);
                    } else if (handle.startsWith(NodeTypeEnum.CONDITION_SWITCH_INTENT_CHAIN.getValue())) {
                        sourceNode.getFailNodes().add(targetNode);
                    }
                } else {
                    sourceNode.getNextNodes().add(targetNode);
                }
            }
            return startNode;
        } catch (Exception e) {
            log.error("Failed to build node execute chain: {}", e.getMessage());
            throw new NodeCustomException(ErrorCode.INVALID_NODE_CONFIGURATION);
        }
    }
}
