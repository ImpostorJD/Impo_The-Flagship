package com.the.flagship.jdt.flagship.Advisor;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.the.flagship.jdt.flagship.dto.PromptAuditRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

@Component 
public class AuditAdvisor implements CallAdvisor {
    private static final Logger LOG = LoggerFactory.getLogger(AuditAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Instant start = Instant.now();
        String userPrompt = request.prompt().getUserMessage().getText();

        ChatClientResponse response = null;
        boolean success = true;
        String error = null;

        try {
            response = chain.nextCall(request);
            return response;
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
            throw e;
        } finally {
            String systemPrompt = request.prompt().getInstructions().stream()
                .filter(m -> m instanceof SystemMessage)
                .map(arg0 -> arg0 != null ? arg0.getText() : null)
                .findFirst()
                .orElse(null);
            long latency = Duration.between(start, Instant.now()).toMillis();

            String responseText = (response != null)
                    ? response.chatResponse().getResult().getOutput().getText()
                    : null;

            String model = (response != null)
                    ? response.chatResponse().getMetadata().getModel()
                    : null;

            Integer promptTokens = (response != null && response.chatResponse().getMetadata().getUsage() != null)
                    ? response.chatResponse().getMetadata().getUsage().getPromptTokens()
                    : null;

            Integer completionTokens = (response != null && response.chatResponse().getMetadata().getUsage() != null)
                    ? response.chatResponse().getMetadata().getUsage().getCompletionTokens() 
                    : null;

            PromptAuditRecord auditRecord = new PromptAuditRecord(
                    UUID.randomUUID().toString(),
                    "chatClient",              // or derive this dynamically, see note below
                    model,
                    systemPrompt,                       // system prompt - see note below
                    userPrompt,
                    responseText,
                    start,
                    latency,
                    promptTokens,
                    completionTokens,
                    success,
                    error
            );

            LOG.debug("PROMPT_AUDIT {}", auditRecord.toString());
            // persist auditRecord here (JPA repo, file, whatever) once you're ready
        }
    }

    @Override
    public String getName() {
        return "audit-advisor";
    }

	@Override
	public int getOrder() {
		return 0; 
	}
}
