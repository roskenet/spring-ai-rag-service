package com.zalando.rag.service;

import java.util.Map;

/**
 * Abstraction for different LLM provider authentication methods.
 *
 * <p>Supports various authentication approaches: - JWT tokens (zLLM) - AWS API keys (Bedrock) - AWS
 * service accounts (Bedrock in K8s) - OAuth flows
 */
public interface AuthenticationProvider {

  /**
   * Checks if the provider is properly authenticated and ready to make API calls.
   *
   * @return true if authentication is valid and working
   */
  boolean isAuthenticated();

  /**
   * Gets the provider type identifier.
   *
   * @return provider type (e.g., "zllm", "bedrock")
   */
  String getProviderType();

  /**
   * Gets authentication information for logging and debugging.
   *
   * @return map containing authentication details (sensitive data should be masked)
   */
  Map<String, String> getAuthenticationInfo();
}
