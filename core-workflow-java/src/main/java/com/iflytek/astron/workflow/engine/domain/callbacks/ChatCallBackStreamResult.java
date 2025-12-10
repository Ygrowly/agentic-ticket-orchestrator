package com.iflytek.astron.workflow.engine.domain.callbacks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data structure for chat callback streaming results.
 * <p>
 * This class encapsulates the result of a node execution in a streaming context,
 * including the node identifier, generated content, and completion status.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCallBackStreamResult {
    /**
     * Unique identifier of the executed node.
     */
    private String nodeId;

    /**
     * Generated content from the node execution.
     */
    private LLMGenerate nodeAnswerContent;

    /**
     * Reason for node completion. 'stop' indicates normal completion, empty string otherwise.
     */
    private String finishReason = "";
}