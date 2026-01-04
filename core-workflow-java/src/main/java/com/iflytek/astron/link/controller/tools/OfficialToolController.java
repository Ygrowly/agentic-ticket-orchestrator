package com.iflytek.astron.link.controller.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iflytek.astron.link.controller.vo.res.ToolManagerResponse;
import com.iflytek.astron.workflow.engine.VariablePool;
import com.iflytek.astron.workflow.engine.context.EngineContextHolder;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.integration.plugins.PluginServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * 官方工具的调用入口
 *
 * @author YiHui
 * @date 2026/1/4
 */
@Slf4j
@RestController
@RequestMapping(path = "/aitools/v1")
public class OfficialToolController {

    @Autowired
    private PluginServiceClient pluginServiceClient;

    /**
     * 智能语音合成，直接服用工作流中的实现
     *
     * @return
     */
    @PostMapping(path = "/smarttts")
    public ToolManagerResponse tts(@RequestBody TtsParam ttsParam) throws Exception {
        try {
            final String ttsDebugId = "smarttts-debug";
            EngineContextHolder.initContext(ttsDebugId, ttsDebugId + System.currentTimeMillis(), null);
            Node node = new Node();
            node.setId(ttsDebugId);
            NodeState nodeState = new NodeState(node, new VariablePool(), null);
            Map<String, Object> inputs = Map.of("text", ttsParam.text(), "vcn", ttsParam.vcn(), "speed", ttsParam.speed());
            Map<String, Object> ans = pluginServiceClient.getTtsIntegration().call(nodeState, inputs);
            return JSONObject.parseObject(JSON.toJSONString(ans), ToolManagerResponse.class);
        } finally {
            EngineContextHolder.remove();
        }
    }

    public record TtsParam(String text, String vcn, String speed) {
    }

}
