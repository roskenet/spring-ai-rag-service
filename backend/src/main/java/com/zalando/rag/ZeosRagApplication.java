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

  public ZeosRagApplication(Environment environment) {
    this.environment = environment;
  }

  public static void main(String[] args) {
    SpringApplication.run(ZeosRagApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    String[] activeProfiles = environment.getActiveProfiles();
    log.info("=== ZEOS RAG APPLICATION STARTED (BEDROCK) ===");
    log.info(
        "Active profiles: {}",
        activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");

    // Log AWS Bedrock configuration
    String region = environment.getProperty("spring.ai.bedrock.aws.region");
    String chatModel = environment.getProperty("spring.ai.bedrock.converse.chat.options.model");
    String embeddingModel = environment.getProperty("spring.ai.bedrock.embedding.options.model");
    String dimensions = environment.getProperty("spring.ai.bedrock.embedding.options.dimensions");

    log.info("AWS Bedrock region: {}", region);
    log.info("Chat model: {}", chatModel);
    log.info("Embedding model: {}", embeddingModel);
    log.info("Embedding dimensions: {}", dimensions);
    log.info("=============================================");
  }
}
