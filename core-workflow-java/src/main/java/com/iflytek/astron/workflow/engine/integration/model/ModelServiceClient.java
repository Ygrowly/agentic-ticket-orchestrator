package com.iflytek.astron.workflow.engine.integration.model;

import com.iflytek.astron.workflow.engine.integration.model.bo.LlmCallback;
import com.iflytek.astron.workflow.engine.integration.model.bo.LlmReqBo;
import com.iflytek.astron.workflow.engine.integration.model.bo.LlmResVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Model service client.
 * Calls console-hub's model API to execute LLM inference.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Slf4j
@Service
public class ModelServiceClient {

    @Autowired
    private OpenAiStyleLlmIntegration llmIntergration;


    /**
     * Call LLM for chat completion.
     *      
     * @param req model ID from database
     */
    public LlmResVo chatCompletion(LlmReqBo req, LlmCallback callback) {
        // TODO: Mock implementation for testing - remove this and uncomment real implementation
        log.warn("Using MOCK LLM response for testing");
        log.info("Mock LLM call: {}", req);

        boolean mock = false;
        if (mock) {
            String mockResponse = """
                    大家好，欢迎来到《沉默王二的编程人生》播客！今天我们要聊的是一个在编程世界里举足轻重的语言——Java。
                                    
                    Java，这个由Sun Microsystems在1995年推出的编程语言，可以说是改变了整个软件开发的格局。它的设计理念"一次编写，到处运行"让无数开发者为之倾倒。
                                    
                    Java不仅仅是一门语言，它更是一个完整的生态系统。从企业级应用到安卓开发，从大数据处理到云计算，Java的身影无处不在。
                                    
                    作为一名资深的Java开发者，我深深感受到Java带给我们的不仅是技术上的成长，更是一种解决问题的思维方式。它的面向对象特性、强大的类库支持，以及庞大的开源社区，都让Java成为了许多程序员的首选。
                                    
                    好了，关于Java的介绍就到这里。如果你对Java编程感兴趣，记得关注我的播客，我们下期再见！
                    """;

            log.info("Mock LLM response length: {}", mockResponse.length());
            return new LlmResVo(new EmptyUsage(), mockResponse, "");
        }

        return llmIntergration.call(req, callback);
    }
}
