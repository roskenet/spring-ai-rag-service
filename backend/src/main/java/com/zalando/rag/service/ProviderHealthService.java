package com.zalando.rag.service;

import com.zalando.rag.dto.ProviderHealthStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for checking the health of RAG providers.
 *
 * <p>Performs comprehensive health checks including: - Authentication validation - Chat model
 * functionality - Embedding model functionality - Response time measurement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderHealthService {

  private final List<AuthenticationProvider> authProviders;
  private final ChatModel chatModel;
  private final EmbeddingModel embeddingModel;

  @Value("${rag.provider}")
  private String activeProvider;

  /**
   * Performs a comprehensive health check of the active provider.
   *
   * @return detailed health status
   */
  public ProviderHealthStatus checkProviderHealth() {
    var startTime = Instant.now();
    var builder = ProviderHealthStatus.builder().provider(activeProvider).checkedAt(startTime);

    try {
      // Find the active authentication provider
      var authProvider =
          authProviders.stream()
              .filter(provider -> provider.getProviderType().equals(activeProvider))
              .findFirst();

      if (authProvider.isEmpty()) {
        log.warn("No authentication provider found for active provider: {}", activeProvider);
        return builder
            .healthy(false)
            .error("No authentication provider found for: " + activeProvider)
            .responseTimeMs(calculateResponseTime(startTime))
            .build();
      }

      // Check authentication
      boolean authWorking = authProvider.get().isAuthenticated();
      builder.authenticationWorking(authWorking);
      builder.authenticationInfo(authProvider.get().getAuthenticationInfo());

      if (!authWorking) {
        log.warn("Authentication failed for provider: {}", activeProvider);
        return builder
            .healthy(false)
            .chatModelWorking(false)
            .embeddingModelWorking(false)
            .error("Authentication failed")
            .responseTimeMs(calculateResponseTime(startTime))
            .build();
      }

      // Test chat model with a simple prompt
      boolean chatWorking = testChatModel();
      builder.chatModelWorking(chatWorking);

      // Test embedding model with a simple text
      boolean embeddingWorking = testEmbeddingModel();
      builder.embeddingModelWorking(embeddingWorking);

      // Overall health status
      boolean overallHealthy = authWorking && chatWorking && embeddingWorking;
      builder.healthy(overallHealthy);

      // Add metadata
      builder.metadata(
          Map.of(
              "chatModelClass", chatModel.getClass().getSimpleName(),
              "embeddingModelClass", embeddingModel.getClass().getSimpleName(),
              "provider", activeProvider));

      var responseTime = calculateResponseTime(startTime);
      builder.responseTimeMs(responseTime);

      if (overallHealthy) {
        log.info("Provider {} health check passed in {}ms", activeProvider, responseTime);
      } else {
        log.warn(
            "Provider {} health check failed - chat: {}, embedding: {}",
            activeProvider,
            chatWorking,
            embeddingWorking);
      }

      return builder.build();

    } catch (Exception e) {
      log.error("Provider health check failed for {}: {}", activeProvider, e.getMessage(), e);
      return builder
          .healthy(false)
          .chatModelWorking(false)
          .embeddingModelWorking(false)
          .authenticationWorking(false)
          .error("Health check exception: " + e.getMessage())
          .responseTimeMs(calculateResponseTime(startTime))
          .build();
    }
  }

  /**
   * Tests the chat model with a simple prompt.
   *
   * @return true if chat model responds successfully
   */
  private boolean testChatModel() {
    try {
      log.debug("Testing chat model for provider: {}", activeProvider);
      var response = chatModel.call(new Prompt("Test connection - respond with 'OK'"));
      boolean success = response != null && response.getResult() != null;

      if (success) {
        log.debug("Chat model test successful");
      } else {
        log.warn("Chat model test failed - null response");
      }

      return success;

    } catch (Exception e) {
      log.warn("Chat model test failed for {}: {}", activeProvider, e.getMessage());
      return false;
    }
  }

  /**
   * Tests the embedding model with simple text.
   *
   * @return true if embedding model responds successfully
   */
  private boolean testEmbeddingModel() {
    try {
      log.debug("Testing embedding model for provider: {}", activeProvider);
      var request = new EmbeddingRequest(List.of("test embedding"), null);
      var response = embeddingModel.call(request);
      boolean success =
          response != null
              && response.getResults() != null
              && !response.getResults().isEmpty()
              && response.getResults().get(0).getOutput() != null;

      if (success) {
        log.debug("Embedding model test successful");
      } else {
        log.warn("Embedding model test failed - invalid response");
      }

      return success;

    } catch (Exception e) {
      log.warn("Embedding model test failed for {}: {}", activeProvider, e.getMessage());
      return false;
    }
  }

  /**
   * Gets a quick health status without performing full model tests.
   *
   * @return basic health status focusing on authentication
   */
  public ProviderHealthStatus getQuickHealthStatus() {
    var startTime = Instant.now();

    try {
      var authProvider =
          authProviders.stream()
              .filter(provider -> provider.getProviderType().equals(activeProvider))
              .findFirst();

      if (authProvider.isEmpty()) {
        return ProviderHealthStatus.unhealthy(activeProvider, "No auth provider found");
      }

      boolean authWorking = authProvider.get().isAuthenticated();
      var responseTime = calculateResponseTime(startTime);

      return ProviderHealthStatus.builder()
          .provider(activeProvider)
          .healthy(authWorking)
          .authenticationWorking(authWorking)
          .authenticationInfo(authProvider.get().getAuthenticationInfo())
          .responseTimeMs(responseTime)
          .metadata(Map.of("checkType", "quick", "provider", activeProvider))
          .build();

    } catch (Exception e) {
      return ProviderHealthStatus.unhealthy(activeProvider, e.getMessage());
    }
  }

  private long calculateResponseTime(Instant startTime) {
    return Instant.now().toEpochMilli() - startTime.toEpochMilli();
  }
}
