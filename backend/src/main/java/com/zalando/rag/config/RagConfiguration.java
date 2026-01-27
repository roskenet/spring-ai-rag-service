package com.zalando.rag.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

  @Bean
  public ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder().build();
  }
}
