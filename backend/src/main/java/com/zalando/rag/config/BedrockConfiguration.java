package com.zalando.rag.config;

import com.zalando.rag.service.BedrockAuthenticationProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for AWS Bedrock LLM provider.
 *
 * <p>This configuration is activated when rag.provider=bedrock and relies on Spring AI's
 * auto-configuration to provide ChatModel and EmbeddingModel beans.
 *
 * <p>Spring AI automatically configures Bedrock models based on application properties.
 * Authentication supports multiple methods: 1. AWS Access Key/Secret (AWS_ACCESS_KEY_ID,
 * AWS_SECRET_ACCESS_KEY) 2. AWS service account (K8s IRSA, EC2 instance profiles) 3. AWS default
 * credentials chain
 */
@Configuration
@EnableConfigurationProperties({RagProviderProperties.class})
@Profile("!test") // Exclude from test profile
@RequiredArgsConstructor
@Slf4j
public class BedrockConfiguration {

  private final BedrockAuthenticationProvider authProvider;
  private final RagProviderProperties ragProperties;

  @PostConstruct
  public void logBedrockConfiguration() {
    var bedrockConfig = ragProperties.getProviders().getBedrock();
    var authInfo = authProvider.getAuthenticationInfo();

    log.info("AWS Bedrock configuration activated");
    log.info("Chat model: {}", bedrockConfig.getModels().getChat());
    log.info("Embedding model: {}", bedrockConfig.getModels().getEmbedding());
    log.info("AWS region: {}", bedrockConfig.getRegion());
    log.info("Authentication method: {}", authInfo.get("authMethod"));
    log.info("Authentication status: {}", authProvider.isAuthenticated() ? "SUCCESS" : "FAILED");

    // Spring AI auto-configuration will handle creating the ChatModel and EmbeddingModel beans
    // based on the application.yaml properties
    log.info(
        "Spring AI auto-configuration will create ChatModel and EmbeddingModel beans from application properties");
  }
}
