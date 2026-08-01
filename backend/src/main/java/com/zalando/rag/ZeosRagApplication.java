package com.zalando.rag;

import com.zalando.rag.config.RagProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableConfigurationProperties({RagProviderProperties.class})
@Slf4j
public class ZeosRagApplication {

  private final Environment environment;
  private final RagProviderProperties ragProviderProperties;

  public ZeosRagApplication(
      Environment environment, RagProviderProperties ragProviderProperties) {
    this.environment = environment;
    this.ragProviderProperties = ragProviderProperties;
  }

  public static void main(String[] args) {
    SpringApplication.run(ZeosRagApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    String[] activeProfiles = environment.getActiveProfiles();
    String activeProvider = ragProviderProperties.getProvider();
    RagProviderProperties.ProviderConfig providerConfig = getProviderConfig(activeProvider);

    log.info("=== ZEOS RAG APPLICATION STARTED ({}) ===", activeProvider.toUpperCase());
    log.info(
       "Active profiles: {}",
       activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");

    log.info("Active provider: {}", activeProvider);
    log.info("Chat model: {}", providerConfig.getModels().getChat());
    log.info("Embedding model: {}", providerConfig.getModels().getEmbedding());
    log.info("Embedding dimensions: {}", providerConfig.getDimensions());
    log.info("Region: {}", providerConfig.getRegion());
    log.info("=============================================");
  }

  private RagProviderProperties.ProviderConfig getProviderConfig(String provider) {
    if (provider == null) {
     return ragProviderProperties.getProviders().getZllm();
    }
    return switch (provider.toLowerCase()) {
     case "bedrock" -> ragProviderProperties.getProviders().getBedrock();
     case "zllm" -> ragProviderProperties.getProviders().getZllm();
     default -> ragProviderProperties.getProviders().getZllm();
    };
  }
}
