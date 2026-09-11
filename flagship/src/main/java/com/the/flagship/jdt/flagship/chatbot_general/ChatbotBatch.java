package com.the.flagship.jdt.flagship.chatbot_general;

import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component 
@ConditionalOnProperty(name = "app.runner", havingValue = "simple-chat-batch")
class ChatbotBatch implements CommandLineRunner {

    private final ChatClient chatClient;
    private final ConfigurableApplicationContext context;
    private final Logger LOG = (Logger) LoggerFactory.getLogger(ChatbotBatch.class); 

    public ChatbotBatch (
        @Qualifier("chatClient") ChatClient chatClient,
        // ChatClient.Builder chatBuilder, 
        ConfigurableApplicationContext context) {
        LOG.info("Chat instantiated, building client and context");
        // this.chatClient = chatBuilder.build();
        this.chatClient = chatClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
       try (Scanner scan = new Scanner(System.in)) {
        LOG.info("Starting chat application");
        System.out.println("Hello welcome to my chatbot");
        boolean chatActive = true;
        while (chatActive) {
            System.out.print("USER INPUT: ");
            String input = scan.nextLine();
            
            if ("exit".equalsIgnoreCase(input)) {
                chatActive = false;
                continue;
            }      
            
             String response = showTypingIndicatorWhile(() ->
                chatClient.prompt().user(input).call().content()
            );

            System.out.println("BOT: " + response);
            
        }

        LOG.info("Closing chat application.");
        context.close();
       }
    }
   
    private String showTypingIndicatorWhile(Callable<String> task) throws Exception {
        AtomicBoolean done = new AtomicBoolean(false);

        Thread spinner = new Thread(() -> {
            String[] frames = { "|", "/", "-", "\\" };
            int i = 0;
            while (!done.get()) {
                System.out.print("\rBOT is thinking " + frames[i % frames.length]);
                i++;
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        spinner.start();
        try {
            return task.call();
        } finally {
            done.set(true);
            spinner.interrupt();
            spinner.join();
            System.out.print("\r" + " ".repeat(20) + "\r"); // clear the spinner line
        }
    }

}