package com.zalando.rag;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RagTestConfiguration {

  public static final String MOCK_CHAT_RESPONSE = "Mock answer from test model";

  @Bean
  @Primary
  public ChatModel testChatModel() {
    return new ChatModel() {
      @Override
      public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(MOCK_CHAT_RESPONSE))));
      }

      @Override
      public String call(String message) {
        return MOCK_CHAT_RESPONSE;
      }
    };
  }

  @Bean
  @Primary
  public EmbeddingModel testEmbeddingModel() {
    return new EmbeddingModel() {
      @Override
      public EmbeddingResponse call(EmbeddingRequest request) {
        return null;
      }

      @Override
      public float[] embed(Document document) {
        return new float[] {0.1f, 0.2f, 0.3f};
      }
    };
  }
}
