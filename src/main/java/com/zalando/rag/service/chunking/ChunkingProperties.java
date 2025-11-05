package com.zalando.rag.service.chunking;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for chunking strategies and document-type specific settings. Supports
 * YAML configuration to customize chunking behavior for different document types and strategies.
 *
 * <p>Example configuration in application.yml:
 *
 * <pre>
 * rag:
 *   chunking:
 *     default-strategy: intelligent
 *     strategies:
 *       intelligent:
 *         max-chunk-size: 2000
 *         preferred-chunk-size: 800
 *         overlap-size: 100
 *       fixed-size:
 *         max-chunk-size: 1000
 *         preferred-chunk-size: 500
 *     document-types:
 *       TECHNICAL_GUIDE:
 *         max-chunk-size: 2500
 *         preserve-code-blocks: true
 *       SIMPLE_TEXT:
 *         max-chunk-size: 800
 *         maintain-sentence-boundaries: false
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "rag.chunking")
@Data
public class ChunkingProperties {

  /** Default chunking strategy to use when none is specified. */
  private String defaultStrategy = "intelligent";

  /** Global configuration that applies to all strategies unless overridden. */
  private ChunkingConfigProperties global = new ChunkingConfigProperties();

  /**
   * Strategy-specific configurations. Key: strategy name, Value: configuration for that strategy
   */
  private Map<String, ChunkingConfigProperties> strategies = new HashMap<>();

  /**
   * Document-type specific configurations. Key: document type name, Value: configuration for that
   * document type
   */
  private Map<String, ChunkingConfigProperties> documentTypes = new HashMap<>();

  /**
   * Gets configuration for a specific document type and strategy combination. Precedence:
   * document-type config > strategy config > global config > defaults
   *
   * @param documentType the document type
   * @param strategyName the strategy name (can be null)
   * @return merged configuration or null if no specific config found
   */
  public ChunkingConfig getConfigForDocumentType(
      DocumentAnalysis.DocumentType documentType, String strategyName) {
    ChunkingConfigProperties config = new ChunkingConfigProperties();

    // Start with global defaults
    mergeConfig(config, global);

    // Apply strategy-specific config if available
    if (strategyName != null && strategies.containsKey(strategyName)) {
      mergeConfig(config, strategies.get(strategyName));
    }

    // Apply document-type specific config if available
    if (documentTypes.containsKey(documentType.name())) {
      mergeConfig(config, documentTypes.get(documentType.name()));
    }

    // Only return a config if we have some non-default values
    if (hasNonDefaultValues(config)) {
      return config.toChunkingConfig();
    }

    return null; // No specific configuration found
  }

  /**
   * Gets configuration for a specific strategy.
   *
   * @param strategyName the strategy name
   * @return configuration for the strategy or null if not configured
   */
  public ChunkingConfig getConfigForStrategy(String strategyName) {
    if (strategies.containsKey(strategyName)) {
      ChunkingConfigProperties config = new ChunkingConfigProperties();
      mergeConfig(config, global);
      mergeConfig(config, strategies.get(strategyName));
      return config.toChunkingConfig();
    }
    return null;
  }

  /**
   * Gets the global default configuration.
   *
   * @return global configuration
   */
  public ChunkingConfig getGlobalConfig() {
    return global.toChunkingConfig();
  }

  private void mergeConfig(ChunkingConfigProperties target, ChunkingConfigProperties source) {
    if (source.getMinChunkSize() != null) target.setMinChunkSize(source.getMinChunkSize());
    if (source.getMaxChunkSize() != null) target.setMaxChunkSize(source.getMaxChunkSize());
    if (source.getPreferredChunkSize() != null)
      target.setPreferredChunkSize(source.getPreferredChunkSize());
    if (source.getOverlapSize() != null) target.setOverlapSize(source.getOverlapSize());
    if (source.getPreserveCodeBlocks() != null)
      target.setPreserveCodeBlocks(source.getPreserveCodeBlocks());
    if (source.getMaintainSentenceBoundaries() != null)
      target.setMaintainSentenceBoundaries(source.getMaintainSentenceBoundaries());
    if (source.getPreserveMarkdownStructure() != null)
      target.setPreserveMarkdownStructure(source.getPreserveMarkdownStructure());
    if (source.getIncludeStructuralMetadata() != null)
      target.setIncludeStructuralMetadata(source.getIncludeStructuralMetadata());
    if (source.getCustomDelimiter() != null) target.setCustomDelimiter(source.getCustomDelimiter());
    if (source.getUseSemanticBoundaries() != null)
      target.setUseSemanticBoundaries(source.getUseSemanticBoundaries());
    if (source.getSemanticSimilarityThreshold() != null)
      target.setSemanticSimilarityThreshold(source.getSemanticSimilarityThreshold());
  }

  private boolean hasNonDefaultValues(ChunkingConfigProperties config) {
    return config.getMinChunkSize() != null
        || config.getMaxChunkSize() != null
        || config.getPreferredChunkSize() != null
        || config.getOverlapSize() != null
        || config.getPreserveCodeBlocks() != null
        || config.getMaintainSentenceBoundaries() != null
        || config.getPreserveMarkdownStructure() != null
        || config.getIncludeStructuralMetadata() != null
        || config.getCustomDelimiter() != null
        || config.getUseSemanticBoundaries() != null
        || config.getSemanticSimilarityThreshold() != null;
  }

  /**
   * Configuration properties that can be set via YAML. Uses nullable Integer/Boolean fields to
   * support partial configuration.
   */
  @Data
  public static class ChunkingConfigProperties {
    private Integer minChunkSize;
    private Integer maxChunkSize;
    private Integer preferredChunkSize;
    private Integer overlapSize;
    private Boolean preserveCodeBlocks;
    private Boolean maintainSentenceBoundaries;
    private Boolean preserveMarkdownStructure;
    private Boolean includeStructuralMetadata;
    private String customDelimiter;
    private Boolean useSemanticBoundaries;
    private Double semanticSimilarityThreshold;

    /** Converts this properties object to a ChunkingConfig, using defaults for null values. */
    public ChunkingConfig toChunkingConfig() {
      ChunkingConfig.ChunkingConfigBuilder builder = ChunkingConfig.builder();

      if (minChunkSize != null) builder.minChunkSize(minChunkSize);
      if (maxChunkSize != null) builder.maxChunkSize(maxChunkSize);
      if (preferredChunkSize != null) builder.preferredChunkSize(preferredChunkSize);
      if (overlapSize != null) builder.overlapSize(overlapSize);
      if (preserveCodeBlocks != null) builder.preserveCodeBlocks(preserveCodeBlocks);
      if (maintainSentenceBoundaries != null)
        builder.maintainSentenceBoundaries(maintainSentenceBoundaries);
      if (preserveMarkdownStructure != null)
        builder.preserveMarkdownStructure(preserveMarkdownStructure);
      if (includeStructuralMetadata != null)
        builder.includeStructuralMetadata(includeStructuralMetadata);
      if (customDelimiter != null) builder.customDelimiter(customDelimiter);
      if (useSemanticBoundaries != null) builder.useSemanticBoundaries(useSemanticBoundaries);
      if (semanticSimilarityThreshold != null)
        builder.semanticSimilarityThreshold(semanticSimilarityThreshold);

      return builder.build();
    }
  }
}
