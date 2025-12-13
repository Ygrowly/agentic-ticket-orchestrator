package com.iflytek.astron.workflow.engine.integration.plugins.tts;

import com.iflytek.astron.workflow.engine.domain.NodeState;

import java.util.Map;

/**
 * @author YiHui
 * @date 2025/12/13
 */
public interface TtsIntegration {
    String source();

    Map<String, Object> call(NodeState nodeState, Map<String, Object> inputs) throws Exception;
}
