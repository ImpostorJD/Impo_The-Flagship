package com.the.flagship.jdt.flagship.chatbot_general;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component 
@ConditionalOnProperty(name = "app.runner", havingValue = "simple-chat-batch")
class ChatbotBatch implements CommandLineRunner {

    private final ChatClient chatClient;
    private final ConfigurableApplicationContext context;

    public ChatbotBatch (ChatClient.Builder chatBuilder, ConfigurableApplicationContext context) {
        this.chatClient = chatBuilder.build();
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
       try (Scanner scan = new Scanner(System.in)) {
        System.out.println("Hello welcome to my chatbot");
        boolean chatActive = true;
        while (chatActive) {
            System.out.print("USER: ");
            String input = scan.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                chatActive = false;
                // context.close();
                context.close();

            }
            // ... call chatClient here
        }
    }
    }

}