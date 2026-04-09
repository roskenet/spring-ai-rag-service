package com.zalando.rag.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Authentication provider for AWS Bedrock.
 *
 * <p>Supports multiple authentication methods: 1. Long-term API key (BEDROCK_API_KEY environment
 * variable) 2. AWS service account (K8s IRSA, EC2 instance profiles) 3. AWS default credentials
 * chain
 *
 * <p>Priority: API key > service account > default credentials
 */
@Component
@ConditionalOnProperty(name = "rag.provider", havingValue = "bedrock")
@Slf4j
public class BedrockAuthenticationProvider implements AuthenticationProvider {

  @Value("${AWS_ACCESS_KEY_ID:}")
  private String awsAccessKeyId;

  @Value("${AWS_SECRET_ACCESS_KEY:}")
  private String awsSecretAccessKey;

  @Value("${AWS_REGION:eu-central-1}")
  private String awsRegion;

  @Override
  public boolean isAuthenticated() {
    try {
      // Check if we have explicit AWS credentials
      if (hasExplicitCredentials()) {
        log.debug("AWS Bedrock authentication using explicit credentials");
        return true;
      }

      // Try AWS default credentials chain (service account, instance profile, etc.)
      DefaultCredentialsProvider.create().resolveCredentials();
      log.debug("AWS Bedrock authentication successful via default credentials chain");
      return true;

    } catch (Exception e) {
      log.warn("AWS Bedrock authentication failed: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public String getProviderType() {
    return "bedrock";
  }

  @Override
  public Map<String, String> getAuthenticationInfo() {
    String authMethod = determineAuthMethod();
    String accessKeyInfo =
        hasExplicitCredentials() ? maskAccessKey(awsAccessKeyId) : "not-provided";

    return Map.of(
        "provider",
        "bedrock",
        "authMethod",
        authMethod,
        "region",
        awsRegion,
        "accessKeyId",
        accessKeyInfo,
        "hasSecretKey",
        String.valueOf(hasSecretKey()),
        "credentialsChain",
        "default-aws-chain");
  }

  /**
   * Checks if explicit AWS credentials are configured.
   *
   * @return true if both AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are set
   */
  public boolean hasExplicitCredentials() {
    return awsAccessKeyId != null
        && !awsAccessKeyId.trim().isEmpty()
        && awsSecretAccessKey != null
        && !awsSecretAccessKey.trim().isEmpty();
  }

  /**
   * Gets the AWS access key ID.
   *
   * @return access key ID if configured, null otherwise
   */
  public String getAccessKeyId() {
    return hasExplicitCredentials() ? awsAccessKeyId.trim() : null;
  }

  /**
   * Gets the AWS secret access key.
   *
   * @return secret access key if configured, null otherwise
   */
  public String getSecretAccessKey() {
    return hasExplicitCredentials() ? awsSecretAccessKey.trim() : null;
  }

  private boolean hasSecretKey() {
    return awsSecretAccessKey != null && !awsSecretAccessKey.trim().isEmpty();
  }

  private String determineAuthMethod() {
    if (hasExplicitCredentials()) {
      return "explicit-credentials";
    }
    // Try to determine if we're in K8s with service account
    if (System.getenv("AWS_ROLE_ARN") != null) {
      return "k8s-service-account";
    }
    if (System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null) {
      return "ecs-task-role";
    }
    return "aws-default-chain";
  }

  private String maskAccessKey(String accessKey) {
    if (accessKey == null || accessKey.length() <= 8) {
      return "****";
    }
    return accessKey.substring(0, 4) + "..." + accessKey.substring(accessKey.length() - 4);
  }
}
