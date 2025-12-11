package com.iflytek.astron.workflow.engine.node.impl.llm;

import com.alibaba.fastjson2.JSONObject;
import com.iflytek.astron.workflow.engine.constants.MsgTypeEnum;
import com.iflytek.astron.workflow.engine.constants.NodeExecStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.domain.callbacks.GenerateUsage;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.domain.chain.OutputItem;
import com.iflytek.astron.workflow.engine.integration.model.LlmChatHistory;
import com.iflytek.astron.workflow.engine.integration.model.ModelServiceClient;
import com.iflytek.astron.workflow.engine.integration.model.bo.LlmReqBo;
import com.iflytek.astron.workflow.engine.integration.model.bo.LlmResVo;
import com.iflytek.astron.workflow.engine.node.AbstractNodeExecutor;
import com.iflytek.astron.workflow.engine.util.VariableTemplateRender;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LLM node executor
 * Calls the model service to execute LLM inference
 */
@Slf4j
@Component
public class LLMNodeExecutor extends AbstractNodeExecutor {

    private final ModelServiceClient modelServiceClient;

    public LLMNodeExecutor(ModelServiceClient modelServiceClient) {
        this.modelServiceClient = modelServiceClient;
    }

    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.LLM;
    }

    @Override
    protected NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs) throws Exception {
        Node node = nodeState.node();
        Map<String, Object> nodeParam = node.getData().getNodeParam();

        Integer modelId = getModelId(nodeParam);

        String resolvedPrompt = getPrompt(nodeParam, inputs);
        String systemPrompt = getSystemPrompt(nodeParam, inputs);
        List<LlmChatHistory.ChatItem> history = getChatHistory(nodeParam, node);
        log.info("LLM node: modelId={}, promptLength={}, history={}", modelId, resolvedPrompt.length(), history.size());
        if (log.isDebugEnabled()) {
            log.debug("LLM prompt (resolved): {}", resolvedPrompt);
        }

        // 这里是执行llm的交互
        LlmReqBo req = JSONObject.parseObject(JSONObject.toJSONString(nodeParam), LlmReqBo.class);
        req.setSystemMsg(systemPrompt);
        req.setUserMsg(resolvedPrompt);
        req.setHistory(history);
        req.setNodeId(node.getId());
        req.setModel((String) nodeParam.get("domain"));


        String chatId = EngineContextHolder.get().getChatId();
        LlmChatHistory.newChat(chatId, node.getId());
        if (StringUtils.isNotBlank(systemPrompt)) {
            LlmChatHistory.addMessage(chatId, node.getId(), MsgTypeEnum.SYSTEM, systemPrompt);
        }
        LlmChatHistory.addMessage(chatId, node.getId(), MsgTypeEnum.USER, resolvedPrompt);
        LlmResVo llmOutput = modelServiceClient.chatCompletion(req, chatResponse -> {
            if (!CollectionUtils.isEmpty(chatResponse.getResults())) {
                nodeState.callback().onNodeProcess(0, node.getId(), node.getData().getNodeMeta().getAliasName(),
                        chatResponse.getResult().getOutput().getText(),
                        (String) chatResponse.getResult().getOutput().getMetadata().get("reasoningContent"));
            }
        });

        LlmChatHistory.addMessage(chatId, node.getId(), MsgTypeEnum.ASSISTANT, llmOutput.content());
        if (StringUtils.isNotBlank(llmOutput.thinkContent())) {
            LlmChatHistory.addMessage(chatId, node.getId(), MsgTypeEnum.THINKING, llmOutput.thinkContent());
        }

        // 返回结果格式化
        Map<String, Object> outputs = formatOutputs(llmOutput, node.getData().getOutputs());

        NodeRunResult result = new NodeRunResult();
        result.setInputs(inputs);
        result.setOutputs(outputs);
        result.setRawOutput(llmOutput.content());
        result.setTokenCost(formatUsage(llmOutput.usage()));
        result.setStatus(NodeExecStatusEnum.SUCCESS);
        return result;
    }

    private Integer getModelId(Map<String, Object> nodeParam) {
        Object modelIdObj = nodeParam.get("modelId");
        if (modelIdObj instanceof Integer) {
            return (Integer) modelIdObj;
        } else if (modelIdObj instanceof String) {
            return Integer.parseInt((String) modelIdObj);
        } else if (modelIdObj instanceof Number) {
            return ((Number) modelIdObj).intValue();
        }
        throw new IllegalArgumentException("Invalid modelId in LLM node: " + modelIdObj);
    }

    /**
     * 获取用户提示词
     *
     * @param nodeParam
     * @param input
     * @return
     */
    private String getPrompt(Map<String, Object> nodeParam, Map<String, Object> input) {
        String userTemplate = getParam(nodeParam, "template");
        if (userTemplate == null) {
            throw new IllegalArgumentException("Missing 'prompt' in LLM node parameters");
        }
        return VariableTemplateRender.render(userTemplate, input);
    }

    /**
     * 获取系统提示词
     *
     * @param nodeParam
     * @param input
     * @return
     */
    private String getSystemPrompt(Map<String, Object> nodeParam, Map<String, Object> input) {
        String sys = getParam(nodeParam, "systemTemplate");
        if (sys == null) {
            return null;
        }
        return VariableTemplateRender.render(sys, input);
    }

    /**
     * 解析请求参数
     *
     * @param nodeParam
     * @param key
     * @return
     */
    private String getParam(Map<String, Object> nodeParam, String key) {
        Object promptObj = nodeParam.get(key);
        if (promptObj == null) {
            return null;
        }
        return String.valueOf(promptObj);
    }

    private List<LlmChatHistory.ChatItem> getChatHistory(Map<String, Object> nodeParam, Node node) {
        int limit = historyLimit(nodeParam);
        if (limit == 0) {
            return List.of();
        }

        // 获取对话历史
        return LlmChatHistory.getHistory(EngineContextHolder.get().getChatId(), node.getId(), limit);
    }

    private Integer historyLimit(Map<String, Object> nodeParam) {
        try {
            Map map = (Map) nodeParam.get("enableChatHistoryV2");
            if (map == null) {
                return 0;
            }
            if (Objects.equals(map.get("isEnabled") + "", "true")) {
                return (Integer) map.get("rounds");
            }
            return 0;
        } catch (Exception e) {
            log.error("parse enableChatHistoryV2 error", e);
            return 0;
        }
    }


    protected Map<String, Object> formatOutputs(LlmResVo llmRes, List<OutputItem> outItems) {
        Map<String, Object> outputs = new HashMap<>();
        if (CollectionUtils.isEmpty(outItems)) {
            outputs.put("output", llmRes.content());
        } else {
            // fixme 先只是先LLM返回text文本的场景; 后续需要考虑结构化、或者图片返回的场景
            OutputItem item = outItems.get(0);
            outputs.put(item.getName(), llmRes.content());

            // 针对思考过程，做一个保留字段的设置，即 reason 表示大模型的推理过程信息
            outItems.stream().filter(s -> "reason".equalsIgnoreCase(s.getName())).findAny().ifPresent(s -> {
                outputs.put(s.getName(), llmRes.thinkContent());
            });
        }
        return outputs;
    }

    private GenerateUsage formatUsage(Usage usage) {
        GenerateUsage generateUsage = new GenerateUsage();
        generateUsage.setCompletionTokens(usage.getCompletionTokens());
        generateUsage.setPromptTokens(usage.getPromptTokens());
        generateUsage.setTotalTokens(usage.getTotalTokens());
        return generateUsage;
    }
}
