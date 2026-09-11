package com.the.flagship.jdt.flagship.dto;

import java.time.Instant;

//Can use record here but i want to use old pojo instead
public class PromptAuditRecord {
    private String id;
    private String clientName;       // "chatClient" or "contractClient"
    private String model;
    private String systemPrompt;
    private String userPrompt;
    private String response;
    private Instant requestedAt;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private boolean success;
    private String errorMessage;
    

    public PromptAuditRecord(
        String id,
        String clientName,       // "chatClient" or "contractClient"
        String model,
        String systemPrompt,
        String userPrompt,
        String response,
        Instant requestedAt,
        Long latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        boolean success,
        String errorMessage
    ) {
        this.id = id;
        this.clientName = clientName;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.response = response;
        this.requestedAt = requestedAt;
        this.latencyMs = latencyMs + 0l;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getId () {
        return this.id;
    }

    public String getClientName() {
        return this.clientName;
    }

    public String getModel () {
        return this.model;
    }

    public String getSystemPrompt() {
        return this.systemPrompt;
    }

    public String getResponse() {
        return this.response;
    }
    public String getUserPrompt() {
        return this.userPrompt;
    }

    public Instant getRequestedAt() {
        return this.requestedAt;
    }
    
    public Long getLatencyMs() {
        return this.latencyMs;
    }

    public Integer getPromptTokens() {
        return this.promptTokens;
    }


    public Integer getCompletionTokens(){
        return this.completionTokens;
    }

    public boolean getSuccess(){
        return this.success;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
public String toString() {
    return "PromptAuditRecord{" +
            "id='" + id + '\'' +
            ", clientName='" + clientName + '\'' +
            ", model='" + model + '\'' +
            ", systemPrompt='" + systemPrompt + '\'' +
            ", userPrompt='" + userPrompt + '\'' +
            ", response='" + response + '\'' +
            ", requestedAt=" + requestedAt +
            ", latencyMs=" + latencyMs +
            ", promptTokens=" + promptTokens +
            ", completionTokens=" + completionTokens +
            ", success=" + success +
            ", errorMessage='" + errorMessage + '\'' +
            '}';
}
}
