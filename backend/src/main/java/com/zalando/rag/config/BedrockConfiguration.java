package com.zalando.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zalando.rag.service.BedrockAuthenticationProvider;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel.InputType;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi.TitanEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Configuration for AWS Bedrock LLM provider.
 *
 * <p>Explicitly creates EmbeddingModel bean to avoid BedrockTitanEmbeddingAutoConfiguration
 * defaulting to amazon.titan-embed-image-v1.
 */
@Configuration
@EnableConfigurationProperties({RagProviderProperties.class})
@Profile("!test") // Exclude from test profile
@RequiredArgsConstructor
@Slf4j
public class BedrockConfiguration {

  private final BedrockAuthenticationProvider authProvider;
  private final RagProviderProperties ragProperties;

  @Value("${AWS_REGION:eu-central-1}")
  private String awsRegion;

  @Bean
  @ConditionalOnMissingBean(EmbeddingModel.class)
  public EmbeddingModel embeddingModel(
      ObjectMapper objectMapper, ObservationRegistry observationRegistry) {
    var api =
        new TitanEmbeddingBedrockApi(
            TitanEmbeddingModel.TITAN_EMBED_TEXT_V2.id(),
            DefaultCredentialsProvider.create(),
            Region.of(awsRegion),
            objectMapper,
            null);
    return new BedrockTitanEmbeddingModel(api, observationRegistry).withInputType(InputType.TEXT);
  }

  @PostConstruct
  public void logBedrockConfiguration() {
    log.info("=== BEDROCK CONFIGURATION DEBUG ===");

    try {
      var bedrockConfig = ragProperties.getProviders().getBedrock();
      var authInfo = authProvider.getAuthenticationInfo();

      log.info("AWS Bedrock configuration activated");
      log.info("Chat model: {}", bedrockConfig.getModels().getChat());
      log.info("Embedding model: {} (explicit bean)", TitanEmbeddingModel.TITAN_EMBED_TEXT_V2.id());
      log.info("AWS region: {}", awsRegion);
      log.info("Authentication method: {}", authInfo.get("authMethod"));
      log.info("Authentication status: {}", authProvider.isAuthenticated() ? "SUCCESS" : "FAILED");

      authInfo.forEach((key, value) -> log.info("Auth info - {}: {}", key, value));

    } catch (Exception e) {
      log.error("Error during Bedrock configuration initialization", e);
      throw e;
    }

    log.info("=== END BEDROCK CONFIGURATION DEBUG ===");
  }
}
