package com.zalando.rag.service;

import com.zalando.rag.entity.RagConfiguration;
import com.zalando.rag.repository.RagConfigurationRepository;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

  private final RagConfigurationRepository configurationRepository;

  // Default configuration values
  private static final String DEFAULT_CONFIG_KEY = "default";
  private static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
  private static final String DEFAULT_CHUNKING_STRATEGY = "intelligent";
  private static final Integer DEFAULT_CHUNK_SIZE = 1000;
  private static final Integer DEFAULT_OVERLAP_PERCENTAGE = 20;
  private static final Double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
  private static final Integer DEFAULT_MAX_RESULTS = 5;
  private static final Boolean DEFAULT_INCLUDE_CITATIONS = true;
  private static final Double DEFAULT_TEMPERATURE = 0.7;
  private static final Integer DEFAULT_TOP_K = 10;
  private static final String DEFAULT_SELECTED_MODEL = "gpt-3.5-turbo";

  @PostConstruct
  public void initializeDefaultConfiguration() {
    try {
      Optional<RagConfiguration> existing = configurationRepository.findDefaultConfiguration();
      if (existing.isEmpty()) {
        RagConfiguration defaultConfig = createDefaultConfiguration();
        configurationRepository.save(defaultConfig);
        log.info("Created default RAG configuration");
      }
    } catch (Exception e) {
      log.error("Failed to initialize default configuration", e);
    }
  }

  private RagConfiguration createDefaultConfiguration() {
    return RagConfiguration.builder()
        .configKey(DEFAULT_CONFIG_KEY)
        .embeddingsModel(DEFAULT_EMBEDDINGS_MODEL)
        .chunkingStrategy(DEFAULT_CHUNKING_STRATEGY)
        .chunkSize(DEFAULT_CHUNK_SIZE)
        .overlapPercentage(DEFAULT_OVERLAP_PERCENTAGE)
        .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
        .maxResults(DEFAULT_MAX_RESULTS)
        .includeCitations(DEFAULT_INCLUDE_CITATIONS)
        .temperature(DEFAULT_TEMPERATURE)
        .topK(DEFAULT_TOP_K)
        .selectedModel(DEFAULT_SELECTED_MODEL)
        .isActive(true)
        .build();
  }

  public RagConfiguration getActiveConfiguration() {
    return configurationRepository
        .findActiveConfiguration()
        .orElseGet(
            () -> {
              log.warn("No active configuration found, creating default");
              return createDefaultConfiguration();
            });
  }

  public RagConfiguration getConfiguration(String configKey) {
    return configurationRepository
        .findByConfigKey(configKey)
        .orElseThrow(() -> new RuntimeException("Configuration not found: " + configKey));
  }

  @Transactional
  public RagConfiguration saveConfiguration(RagConfiguration configuration) {
    try {
      // Deactivate existing active configuration if this one is being set as active
      if (configuration.getIsActive()) {
        Optional<RagConfiguration> existingActive =
            configurationRepository.findActiveConfiguration();
        if (existingActive.isPresent()
            && !existingActive.get().getId().equals(configuration.getId())) {
          RagConfiguration existing = existingActive.get();
          existing.setIsActive(false);
          configurationRepository.save(existing);
        }
      }

      RagConfiguration saved = configurationRepository.save(configuration);
      log.info("Saved configuration: {}", saved.getConfigKey());
      return saved;
    } catch (Exception e) {
      log.error("Failed to save configuration", e);
      throw new RuntimeException("Failed to save configuration", e);
    }
  }

  @Transactional
  public RagConfiguration updateConfiguration(String configKey, RagConfiguration updatedConfig) {
    RagConfiguration existing = getConfiguration(configKey);

    // Update fields
    if (updatedConfig.getEmbeddingsModel() != null) {
      existing.setEmbeddingsModel(updatedConfig.getEmbeddingsModel());
    }
    if (updatedConfig.getChunkingStrategy() != null) {
      existing.setChunkingStrategy(updatedConfig.getChunkingStrategy());
    }
    if (updatedConfig.getChunkSize() != null) {
      existing.setChunkSize(updatedConfig.getChunkSize());
    }
    if (updatedConfig.getOverlapPercentage() != null) {
      existing.setOverlapPercentage(updatedConfig.getOverlapPercentage());
    }
    if (updatedConfig.getSimilarityThreshold() != null) {
      existing.setSimilarityThreshold(updatedConfig.getSimilarityThreshold());
    }
    if (updatedConfig.getMaxResults() != null) {
      existing.setMaxResults(updatedConfig.getMaxResults());
    }
    if (updatedConfig.getIncludeCitations() != null) {
      existing.setIncludeCitations(updatedConfig.getIncludeCitations());
    }
    if (updatedConfig.getTemperature() != null) {
      existing.setTemperature(updatedConfig.getTemperature());
    }
    if (updatedConfig.getTopK() != null) {
      existing.setTopK(updatedConfig.getTopK());
    }
    if (updatedConfig.getSelectedModel() != null) {
      existing.setSelectedModel(updatedConfig.getSelectedModel());
    }
    if (updatedConfig.getIsActive() != null) {
      existing.setIsActive(updatedConfig.getIsActive());
    }

    return saveConfiguration(existing);
  }

  @Transactional
  public void deleteConfiguration(String configKey) {
    if (DEFAULT_CONFIG_KEY.equals(configKey)) {
      throw new RuntimeException("Cannot delete default configuration");
    }

    Optional<RagConfiguration> config = configurationRepository.findByConfigKey(configKey);
    if (config.isPresent()) {
      configurationRepository.delete(config.get());
      log.info("Deleted configuration: {}", configKey);
    }
  }

  @Transactional
  public RagConfiguration setActiveConfiguration(String configKey) {
    RagConfiguration config = getConfiguration(configKey);

    // Deactivate current active configuration
    Optional<RagConfiguration> currentActive = configurationRepository.findActiveConfiguration();
    if (currentActive.isPresent()) {
      RagConfiguration current = currentActive.get();
      current.setIsActive(false);
      configurationRepository.save(current);
    }

    // Activate the specified configuration
    config.setIsActive(true);
    return configurationRepository.save(config);
  }

  // Convenience methods for getting specific configuration values
  public String getActiveEmbeddingsModel() {
    return getActiveConfiguration().getEmbeddingsModel();
  }

  public String getActiveChunkingStrategy() {
    return getActiveConfiguration().getChunkingStrategy();
  }

  public Integer getActiveChunkSize() {
    return getActiveConfiguration().getChunkSize();
  }

  public Double getActiveSimilarityThreshold() {
    return getActiveConfiguration().getSimilarityThreshold();
  }

  public Integer getActiveMaxResults() {
    return getActiveConfiguration().getMaxResults();
  }

  public String getActiveSelectedModel() {
    return getActiveConfiguration().getSelectedModel();
  }

  public Double getActiveTemperature() {
    return getActiveConfiguration().getTemperature();
  }
}
