package com.zalando.rag.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Health status information for RAG providers.
 *
 * <p>Contains detailed health check results for chat models, embedding models, and authentication
 * status.
 */
@Data
@Builder
public class ProviderHealthStatus {

  /** Currently active provider (e.g., "bedrock") */
  private String provider;

  /** Overall health status */
  private boolean healthy;

  /** Chat model functionality status */
  private boolean chatModelWorking;

  /** Embedding model functionality status */
  private boolean embeddingModelWorking;

  /** Authentication status */
  private boolean authenticationWorking;

  /** Response time for health check in milliseconds */
  private Long responseTimeMs;

  /** Timestamp of health check */
  @Builder.Default private Instant checkedAt = Instant.now();

  /** Authentication details (sensitive data should be masked) */
  private Map<String, String> authenticationInfo;

  /** Error message if health check failed */
  private String error;

  /** Additional metadata about the provider */
  private Map<String, Object> metadata;

  /**
   * Creates a healthy status.
   *
   * @param provider the provider name
   * @param authInfo authentication information
   * @return healthy status instance
   */
  public static ProviderHealthStatus healthy(String provider, Map<String, String> authInfo) {
    return ProviderHealthStatus.builder()
        .provider(provider)
        .healthy(true)
        .chatModelWorking(true)
        .embeddingModelWorking(true)
        .authenticationWorking(true)
        .authenticationInfo(authInfo)
        .build();
  }

  /**
   * Creates an unhealthy status with error message.
   *
   * @param provider the provider name
   * @param error error message
   * @return unhealthy status instance
   */
  public static ProviderHealthStatus unhealthy(String provider, String error) {
    return ProviderHealthStatus.builder()
        .provider(provider)
        .healthy(false)
        .chatModelWorking(false)
        .embeddingModelWorking(false)
        .authenticationWorking(false)
        .error(error)
        .build();
  }
}
