package com.the.flagship.jdt.flagship.config;

import com.the.flagship.jdt.flagship.Advisor.AuditAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ChatClientConfig {

    @Value("${spring.ai.openai.chat.options.model}")
    private String chatModel;

    @Value("${spring.ai.openai.chat.options.model}")
    private String contractModel;

    private AuditAdvisor auditAdvisor;

    public ChatClientConfig (AuditAdvisor auditAdvisor) {
        this.auditAdvisor = auditAdvisor;
    }

    @Bean
    @Qualifier("chatClient")
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultOptions(OpenAiChatOptions.builder()
                .model(chatModel))
            .defaultSystem("You are a helpful assistant.")
            .defaultAdvisors(auditAdvisor)
            .build();
    }

    @Bean
    @Qualifier("contractClient")
    ChatClient contractClient(ChatClient.Builder builder) {
        return builder
            .defaultOptions(OpenAiChatOptions.builder()
                .model(contractModel))
            .defaultSystem("You are a contract analysis assistant.")
            .build();
    }
}  