package com.iflytek.astron.workflow.engine;

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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Workflow execution engine
 * Executes workflow nodes in sequential order based on edges
 */
@Slf4j
@Component
public class WorkflowEngine {

    private final Map<NodeTypeEnum, NodeExecutor> nodeExecutors;

    public WorkflowEngine(List<NodeExecutor> executors) {
        this.nodeExecutors = new HashMap<>();
        for (NodeExecutor executor : executors) {
            this.nodeExecutors.put(executor.getNodeType(), executor);
        }
        log.info("Registered {} node executors: {}", nodeExecutors.size(), nodeExecutors.keySet());
    }

    /**
     * Execute a workflow
     *
     * @param workflowDSL workflow definition
     * @param inputs      initial input values (from user)
     * @param callback    stream callback for SSE output
     * @throws Exception if execution fails
     */
    public void execute(WorkflowDSL workflowDSL, VariablePool variablePool, Map<String, Object> inputs, StreamCallback callback) throws Exception {
        log.info("Starting workflow execution with {} nodes", workflowDSL.getNodes().size());

        // 前置校验
        verifyWorkflow(workflowDSL);

        // 清空上下文变量
        variablePool.clear();

        // 创建工作流回调处理器
        Queue<ChatCallBackStreamResult> orderStreamResultQ = new ConcurrentLinkedQueue<>();
        Queue<LLMGenerate> streamQueue = new ConcurrentLinkedQueue<>();

        Node endNode = workflowDSL.getNodes().stream().filter(s -> s.getNodeType() == NodeTypeEnum.END).findFirst().get();
        String sid = FlowUtil.genWorkflowId(workflowDSL.getFlowId());
        WorkflowMsgCallback workflowCallback = new WorkflowMsgCallback(
                sid,
                callback,
                Objects.equals(endNode.getData().getNodeParam().get("outputMode"), 1) ? EndNodeOutputModeEnum.VARIABLE_MODE : EndNodeOutputModeEnum.DIRECT_MODE,
                streamQueue,
                orderStreamResultQ
        );


        // 初始化上下文
        EngineContextHolder.initContext(workflowDSL.getFlowId(), workflowDSL.getUuid(), workflowCallback);

        // 发送工作流开始事件
        workflowCallback.onWorkflowStart();

        try {
            // 构建从起始节点开始的执行链路
            Node startNode = buildNodeExecuteChain(workflowDSL);
            // 初始化启动参数
            initializeStartNodeInputs(startNode, variablePool, inputs);

            // 执行编排的流程节点
            executeNode(startNode, variablePool, workflowCallback);

            log.info("Workflow: {} execution completed successfully", sid);
            // 发送工作流结束事件
            workflowCallback.onWorkflowEnd(new NodeRunResult()); // 这里应该传入实际的结果
        } catch (Exception e) {
            // 发送工作流错误结束事件
            workflowCallback.onWorkflowEnd(new NodeRunResult());
            throw e;
        } finally {
            // 消费所有的消息
            workflowCallback.finished();
            // 移除上下文信息
            EngineContextHolder.remove();
        }
    }

    /**
     * Initialize start node inputs with user-provided values
     */
    private void initializeStartNodeInputs(Node startNode, VariablePool variablePool, Map<String, Object> inputs) {
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            variablePool.set(startNode.getId(), entry.getKey(), entry.getValue());
            if (log.isDebugEnabled()) {
                log.debug("Initialized start node input: {}.{} = {}", startNode.getId(), entry.getKey(), entry.getValue());
            }
        }
    }

    private void verifyWorkflow(WorkflowDSL workflowDSL) {
        // 做一个简单的dsl校验，要求包含起始节点和结束节点，要求每个节点都能找到对应执行器
        if (CollectionUtils.isEmpty(workflowDSL.getNodes()) || CollectionUtils.isEmpty(workflowDSL.getEdges())) {
            throw new IllegalStateException("Invalid workflow DSL: missing start or end node");
        }
        for (Node node : workflowDSL.getNodes()) {
            NodeTypeEnum nodeType = node.getNodeType();
            if (nodeType == null) {
                throw new IllegalStateException("Invalid workflow DSL: node type is null");
            }
            NodeExecutor executor = nodeExecutors.get(nodeType);
            if (executor == null) {
                throw new IllegalStateException("Invalid workflow DSL: no executor found for node type: " + nodeType);
            }
        }

        // 环路检测 (Loop Detection)
        // 使用 Kahn 算法 (基于入度的拓扑排序) 检测图中是否存在环
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
            adjList.get(u).add(v);
            inDegree.put(v, inDegree.getOrDefault(v, 0) + 1);
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



    /**
     * 构建Node执行链路
     *
     * @param workflowDSL
     * @return
     */
    public Node buildNodeExecuteChain(WorkflowDSL workflowDSL) {
        try {
            Node startNode = null;

            // 构建Node Map
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

            // 根据边来构建执行策略
            for (Edge edge : workflowDSL.getEdges()) {
                String sourceNodeId = edge.getSource();
                String targetNodeId = edge.getTarget();
                Node sourceNode = nodeMap.get(sourceNodeId);
                if (sourceNode == null) {
                    throw new IllegalStateException("No node found for source node ID: " + sourceNodeId);
                }
                Node targetNode = nodeMap.get(targetNodeId);
                if (targetNode == null) {
                    throw new IllegalStateException("No node found for target node ID: " + targetNodeId);
                }

                if (targetNode.getPreNodes() == null) {
                    targetNode.setPreNodes(new ArrayList<>());
                }
                targetNode.getPreNodes().add(sourceNode);

                String handle = edge.getSourceHandle();
                if (StringUtils.isNotBlank(handle)) {
                    if (handle.startsWith(NodeTypeEnum.CONDITION_SWITCH_NORMAL_ONE_OF.getValue())) {
                        // 正常响应的执行链路
                        sourceNode.getNextNodes().add(targetNode);
                    } else if (handle.startsWith(NodeTypeEnum.CONDITION_SWITCH_INTENT_CHAIN.getValue())) {
                        // 异常case的执行链路
                        // fixme 尚未完全弄清楚astron_agent的异常场景，先简单实现一个，这里只处理了 intent_chain|fail_one_of 的异常场景
                        sourceNode.getFailNodes().add(targetNode);
                    }

                } else {
                    // 无异常分支流程场景
                    sourceNode.getNextNodes().add(targetNode);
                }
            }
            return startNode;
        } catch (Exception e) {
            // 构建执行链路失败
            log.error("Failed to build node execute chain: {}", e.getMessage());
            throw new NodeCustomException(ErrorCode.INVALID_NODE_CONFIGURATION);
        }
    }


    /**
     * Execute a single node
     */
    private void executeNode(Node node, VariablePool variablePool, WorkflowMsgCallback callback) throws Exception {
        if (node.getStatus().executed()) {
            // todo 我们这里先只实现单次的Node执行策略，后续可以扩展为支持循环执行的场景
            // 已经执行过，则直接返回
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("prepare to executeNode: {}", node.getId());
        }

        // 1. 前置校验： node 执行的前提条件是所有的前置Node都已经执行完毕
        if (!CollectionUtils.isEmpty(node.getPreNodes())) {
            for (Node preNode : node.getPreNodes()) {
                if (!preNode.getStatus().executed()) {
                    executeNode(preNode, variablePool, callback);
                }
            }
        }

        // 2. 如果当前节点为 MARK，则需要判断是否可以执行
        // 比如一个mark节点，有两个前置节点AB，其中A执行成功，则执行当前节点；B为分支节点，执行失败，才执行当前节点；
        // 若B执行成功，会将当前节点标记为SKIP；此时再执行A，执行成功，会到当前节点，正常来说也是可以执行的
        // 判断规则：找到这个节点的所有前置节点，只要当前节点在前置节点的执行分支下，就执行；否则不执行，标记为SKIP
        if (node.getStatus() == NodeStatusEnum.MARK) {
            boolean canExecute = false;
            for (Node preNode : node.getPreNodes()) {
                if (preNode.getStatus() == NodeStatusEnum.SKIP) {
                    continue;
                } else if (preNode.getStatus() == NodeStatusEnum.ERROR) {
                    if (preNode.getFailNodes().contains(node)) {
                        // 在当前分支中，需要正常执行
                        canExecute = true;
                        break;
                    }
                } else if (preNode.getStatus() == NodeStatusEnum.SUCCESS) {
                    if (preNode.getNextNodes().contains(node)) {
                        // 在当前分支中，需要正常执行
                        canExecute = true;
                        break;
                    }
                }
            }
            if (!canExecute) {
                // 不需要执行时
                node.setStatus(NodeStatusEnum.SKIP);
                return;
            }
        }


        // 3. 执行当前节点
        NodeTypeEnum nodeType = node.getNodeType();
        NodeExecutor executor = nodeExecutors.get(nodeType);
        if (executor == null) {
            throw new IllegalStateException("No executor found for node type: " + nodeType);
        }

        node.setStatus(NodeStatusEnum.RUNNING);
        // 执行当前节点
        NodeExecStatusEnum execStatus;
        while (true) {
            NodeRunResult res = executor.execute(new NodeState(node, variablePool, callback));
            execStatus = res.getStatus();
            if (execStatus != NodeExecStatusEnum.ERR_RETRY) {
                break;
            }
        }

        // 4. 节点执行完毕，继续执行后续节点
        if (execStatus == NodeExecStatusEnum.ERR_INTERUPT) {
            // 异常中断整个流程
            node.setStatus(NodeStatusEnum.ERROR);
            throw new NodeCustomException(ErrorCode.INTERRUPTED_ERROR);
        } else if (execStatus == NodeExecStatusEnum.ERR_FAIL_CONDITION) {
            // 节点执行失败，则执行异常处理分支
            node.setStatus(NodeStatusEnum.ERROR);
            executeFailedCondition(node, variablePool, callback);
        } else if (execStatus == NodeExecStatusEnum.ERR_CODE_MSG) {
            // 节点执行失败，但是依然走正常分支
            node.setStatus(NodeStatusEnum.ERROR);
            executeNormalCondition(node, variablePool, callback);
        } else {
            // 节点执行成功
            node.setStatus(NodeStatusEnum.SUCCESS);
            executeNormalCondition(node, variablePool, callback);
        }
    }

    private void executeNormalCondition(Node node, VariablePool variablePool, WorkflowMsgCallback callback) throws Exception {
        // 将异常分支的节点为未执行，则标记为skip；
        for (Node failNode : node.getFailNodes()) {
            if (!failNode.getStatus().executed()) {
                failNode.setStatus(NodeStatusEnum.MARK);
            }
        }
        // 需要注意的是，从正常流程的节点，也可能同样走到这个异常分支的节点，这里需要支持重启这个节点的状态

        // 执行成功 或者 没有异常处理分支的场景
        for (Node nextNode : node.getNextNodes()) {
            executeNode(nextNode, variablePool, callback);
        }
    }

    private void executeFailedCondition(Node node, VariablePool variablePool, WorkflowMsgCallback callback) throws Exception {
        for (Node nextNode : node.getNextNodes()) {
            if (!nextNode.getStatus().executed()) {
                nextNode.setStatus(NodeStatusEnum.MARK);
            }
        }

        // 执行失败
        for (Node failNode : node.getFailNodes()) {
            executeNode(failNode, variablePool, callback);
        }
    }
}
