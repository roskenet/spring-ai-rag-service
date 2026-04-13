package com.zalando.rag.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Authentication provider for AWS Bedrock using AWS default credentials chain.
 *
 * <p>Relies on platform team's AWS infrastructure setup which provides authentication via: - EC2
 * instance profiles - ECS task roles - IAM roles for service accounts (if using EKS) - AWS
 * credentials files - Environment variables (managed by platform)
 */
@Component
@ConditionalOnProperty(name = "rag.provider", havingValue = "bedrock")
@Slf4j
public class BedrockAuthenticationProvider implements AuthenticationProvider {

  @Value("${AWS_REGION:eu-central-1}")
  private String awsRegion;

  @Override
  public boolean isAuthenticated() {
    try {
      // Use AWS default credentials chain (platform team's infrastructure setup)
      var credentials = DefaultCredentialsProvider.create().resolveCredentials();
      log.info(
          "AWS Bedrock authentication successful via default credentials chain - AccessKeyId: {}",
          maskAccessKey(credentials.accessKeyId()));
      return true;

    } catch (Exception e) {
      log.error("AWS Bedrock authentication failed: {}", e.getMessage(), e);
      log.error("AWS region: {}", awsRegion);
      log.error(
          "Platform team's AWS credentials setup not available. Check: EC2 instance profile, ECS task role, IAM roles, or ~/.aws/credentials");
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

    return Map.of(
        "provider",
        "bedrock",
        "authMethod",
        authMethod,
        "region",
        awsRegion,
        "credentialsSource",
        "aws-default-chain",
        "awsRoleArn",
        System.getenv("AWS_ROLE_ARN") != null ? System.getenv("AWS_ROLE_ARN") : "not-set",
        "awsWebIdentityTokenFile",
        System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE") != null ? "present" : "not-set",
        "containerCredentialsUri",
        System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null ? "present" : "not-set");
  }

  private String determineAuthMethod() {
    // Check for Kubernetes service account with IAM role (IRSA/EKS)
    if (System.getenv("AWS_ROLE_ARN") != null
        && System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE") != null) {
      return "k8s-service-account-irsa";
    }
    // Check for ECS task role
    if (System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null) {
      return "ecs-task-role";
    }
    // Check for EC2 instance profile
    if (System.getenv("AWS_CONTAINER_CREDENTIALS_FULL_URI") != null) {
      return "ec2-instance-profile";
    }
    // Default to AWS credentials chain (instance profile, credentials file, environment)
    return "platform-managed-credentials";
  }

  private String maskAccessKey(String accessKey) {
    if (accessKey == null || accessKey.length() <= 8) {
      return "****";
    }
    return accessKey.substring(0, 4) + "..." + accessKey.substring(accessKey.length() - 4);
  }
}
