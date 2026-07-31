package com.zalando.rag.controller;

import com.zalando.rag.config.RagProviderProperties;
import com.zalando.rag.dto.ProviderHealthStatus;
import com.zalando.rag.entity.RagConfiguration;
import com.zalando.rag.service.ConfigurationService;
import com.zalando.rag.service.ProviderHealthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to access configuration endpoints
public class ConfigurationController {

  private final ConfigurationService configurationService;
  private final ProviderHealthService providerHealthService;
  private final RagProviderProperties ragProviderProperties;

  @GetMapping
  public ResponseEntity<RagConfiguration> getActiveConfiguration() {
    try {
      RagConfiguration config = configurationService.getActiveConfiguration();
      return ResponseEntity.ok(config);
    } catch (Exception e) {
      log.error("Error retrieving active configuration", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/{configKey}")
  public ResponseEntity<RagConfiguration> getConfiguration(@PathVariable String configKey) {
    try {
      RagConfiguration config = configurationService.getConfiguration(configKey);
      return ResponseEntity.ok(config);
    } catch (RuntimeException e) {
      log.warn("Configuration not found: {}", configKey);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error retrieving configuration: {}", configKey, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping
  public ResponseEntity<RagConfiguration> createConfiguration(
      @Valid @RequestBody RagConfiguration configuration) {
    try {
      RagConfiguration saved = configurationService.saveConfiguration(configuration);
      return ResponseEntity.ok(saved);
    } catch (Exception e) {
      log.error("Error creating configuration", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PutMapping("/{configKey}")
  public ResponseEntity<RagConfiguration> updateConfiguration(
      @PathVariable String configKey, @Valid @RequestBody RagConfiguration configuration) {
    try {
      RagConfiguration updated = configurationService.updateConfiguration(configKey, configuration);
      return ResponseEntity.ok(updated);
    } catch (RuntimeException e) {
      log.warn("Configuration not found for update: {}", configKey);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error updating configuration: {}", configKey, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PutMapping("/{configKey}/activate")
  public ResponseEntity<RagConfiguration> activateConfiguration(@PathVariable String configKey) {
    try {
      RagConfiguration activated = configurationService.setActiveConfiguration(configKey);
      return ResponseEntity.ok(activated);
    } catch (RuntimeException e) {
      log.warn("Configuration not found for activation: {}", configKey);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error activating configuration: {}", configKey, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping("/{configKey}")
  public ResponseEntity<Void> deleteConfiguration(@PathVariable String configKey) {
    try {
      configurationService.deleteConfiguration(configKey);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      log.warn("Cannot delete configuration: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error deleting configuration: {}", configKey, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // Convenience endpoints for specific configuration values
  @GetMapping("/embeddings-model")
  public ResponseEntity<String> getActiveEmbeddingsModel() {
    try {
      String model = configurationService.getActiveEmbeddingsModel();
      return ResponseEntity.ok(model);
    } catch (Exception e) {
      log.error("Error retrieving active embeddings model", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/chunking-strategy")
  public ResponseEntity<String> getActiveChunkingStrategy() {
    try {
      String strategy = configurationService.getActiveChunkingStrategy();
      return ResponseEntity.ok(strategy);
    } catch (Exception e) {
      log.error("Error retrieving active chunking strategy", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/similarity-threshold")
  public ResponseEntity<Double> getActiveSimilarityThreshold() {
    try {
      Double threshold = configurationService.getActiveSimilarityThreshold();
      return ResponseEntity.ok(threshold);
    } catch (Exception e) {
      log.error("Error retrieving active similarity threshold", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/selected-model")
  public ResponseEntity<String> getActiveSelectedModel() {
    try {
      String model = configurationService.getActiveSelectedModel();
      return ResponseEntity.ok(model);
    } catch (Exception e) {
      log.error("Error retrieving active selected model", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // Provider health and status endpoints
  @GetMapping("/provider-health")
  public ResponseEntity<ProviderHealthStatus> getProviderHealth() {
    try {
      ProviderHealthStatus status = providerHealthService.checkProviderHealth();
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error checking provider health", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/provider-health/quick")
  public ResponseEntity<ProviderHealthStatus> getQuickProviderHealth() {
    try {
      ProviderHealthStatus status = providerHealthService.getQuickHealthStatus();
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("Error checking quick provider health", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/active-provider")
  public ResponseEntity<Map<String, Object>> getActiveProvider() {
    try {
      var activeProvider = ragProviderProperties.getProvider();
      var providerConfig = ragProviderProperties.getProviders().getBedrock();

      var response =
          Map.of(
              "provider", activeProvider,
              "models",
                  Map.of(
                      "chat", providerConfig.getModels().getChat(),
                      "embedding", providerConfig.getModels().getEmbedding()),
              "dimensions", providerConfig.getDimensions(),
              "region", providerConfig.getRegion(),
              "enabled", providerConfig.isEnabled());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error retrieving active provider information", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
