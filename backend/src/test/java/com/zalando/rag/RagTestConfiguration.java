package com.zalando.rag;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mock ChatModel and EmbeddingModel beans for testing.
 * These @Primary beans override any auto-configured AI model beans during tests.
 */
@TestConfiguration
public class RagTestConfiguration {

  @Bean
  @Primary
  public ChatModel testChatModel() {
    return new ChatModel() {
      @Override
      public ChatResponse call(Prompt prompt) {
        // Mock implementation for tests
        return null;
      }

      @Override
      public String call(String message) {
        return "Mock response for: " + message;
      }
    };
  }

  @Bean
  @Primary
  public EmbeddingModel testEmbeddingModel() {
    return new EmbeddingModel() {
      @Override
      public EmbeddingResponse call(EmbeddingRequest request) {
        // Mock implementation for tests
        return null;
      }

      @Override
      public float[] embed(Document document) {
        // Mock implementation for tests
        return new float[] {0.1f, 0.2f, 0.3f};
      }
    };
  }
}
