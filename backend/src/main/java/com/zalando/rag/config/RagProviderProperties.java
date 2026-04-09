package com.zalando.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for RAG provider settings. Supports dual provider architecture with zLLM
 * and AWS Bedrock.
 */
@ConfigurationProperties(prefix = "rag")
@Data
public class RagProviderProperties {

  /** Currently active provider: 'zllm' or 'bedrock' */
  private String provider = "zllm"; // Default to existing

  /** Provider-specific configurations */
  private Providers providers = new Providers();

  @Data
  public static class Providers {
    private ProviderConfig zllm = new ProviderConfig();
    private ProviderConfig bedrock = new ProviderConfig();
  }

  @Data
  public static class ProviderConfig {
    private boolean enabled = true;
    private Models models = new Models();
    private int dimensions = 1536;
    private String region = "eu-central-1";
  }

  @Data
  public static class Models {
    private String chat;
    private String embedding;
  }
}
