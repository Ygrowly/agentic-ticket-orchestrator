package com.iflytek.astron.workflow.engine.domain.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage information for text generation.
 * 
 * This class tracks the number of tokens used in prompts, completions,
 * and total usage for billing and monitoring purposes.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
public class GenerateUsage {
    /**
     * Number of tokens in the generated completion.
     */
    @JsonProperty("completion_tokens")
    private int completionTokens = 0;
    
    /**
     * Number of tokens in the prompt.
     */
    @JsonProperty("prompt_tokens")
    private int promptTokens = 0;
    
    /**
     * Total number of tokens used in the request (prompt + completion).
     */
    @JsonProperty("total_tokens")
    private int totalTokens = 0;
    
    public GenerateUsage() {
    }
    
    public GenerateUsage(int completionTokens, int promptTokens, int totalTokens) {
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }
    
    /**
     * Add another usage instance to this one.
     * 
     * @param usage Another GenerateUsage instance to add
     */
    public void add(GenerateUsage usage) {
        this.completionTokens += usage.completionTokens;
        this.promptTokens += usage.promptTokens;
        this.totalTokens += usage.totalTokens;
    }
    
    // Getters and setters
    public int getCompletionTokens() {
        return completionTokens;
    }
    
    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }
    
    public int getPromptTokens() {
        return promptTokens;
    }
    
    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
}