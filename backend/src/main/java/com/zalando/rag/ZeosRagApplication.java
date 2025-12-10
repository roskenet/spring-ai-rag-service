package com.zalando.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
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
    log.info("=== ZEOS RAG APPLICATION STARTED ===");
    log.info(
        "Active profiles: {}",
        activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");

    // Log embedding configuration
    String baseUrl = environment.getProperty("spring.ai.openai.base-url");
    String model = environment.getProperty("spring.ai.openai.embedding.options.model");
    String dimensions = environment.getProperty("spring.ai.openai.embedding.options.dimensions");

    log.info("OpenAI base-url: {}", baseUrl);
    log.info("Embedding model: {}", model);
    log.info("Embedding dimensions: {}", dimensions);
    log.info("=====================================");
  }
}
