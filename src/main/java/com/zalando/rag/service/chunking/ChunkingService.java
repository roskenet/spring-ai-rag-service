package com.zalando.rag.service.chunking;

import com.zalando.rag.service.chunking.util.MetadataUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * Main service for document chunking that coordinates between different chunking strategies. This
 * service acts as a facade, providing a simple interface while delegating to appropriate chunking
 * strategies based on document analysis and configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkingService {

  private final DocumentAnalysisService documentAnalysisService;
  private final ChunkingStrategyRegistry strategyRegistry;
  private final ChunkingProperties chunkingProperties;

  /**
   * Chunks a document using the default strategy and configuration.
   *
   * @param content the document content to chunk
   * @param filename the filename for metadata
   * @param title the document title for metadata
   * @return list of document chunks
   */
  public List<Document> chunkDocument(String content, String filename, String title) {
    return chunkDocument(content, filename, title, null, null);
  }

  /**
   * Chunks a document with explicit strategy selection.
   *
   * @param content the document content to chunk
   * @param filename the filename for metadata
   * @param title the document title for metadata
   * @param strategyName specific strategy to use (null for auto-selection)
   * @return list of document chunks
   */
  public List<Document> chunkDocument(
      String content, String filename, String title, String strategyName) {
    return chunkDocument(content, filename, title, strategyName, null);
  }

  /**
   * Chunks a document with full control over strategy and configuration.
   *
   * @param content the document content to chunk
   * @param filename the filename for metadata
   * @param title the document title for metadata
   * @param strategyName specific strategy to use (null for auto-selection)
   * @param config chunking configuration (null for default based on document type)
   * @return list of document chunks
   */
  public List<Document> chunkDocument(
      String content, String filename, String title, String strategyName, ChunkingConfig config) {

    if (content == null || content.trim().isEmpty()) {
      log.warn("Empty or null content provided for document: {}", filename);
      return List.of();
    }

    long startTime = System.currentTimeMillis();

    // Analyze the document to understand its characteristics
    DocumentAnalysis analysis = documentAnalysisService.analyzeDocument(content);
    log.debug("Document analysis for {}: {}", filename, analysis);

    // Select the appropriate strategy
    ChunkingStrategy strategy = strategyRegistry.selectStrategy(analysis, strategyName);
    log.info(
        "Selected chunking strategy '{}' for document: {}", strategy.getStrategyName(), filename);

    // Determine effective configuration
    ChunkingConfig effectiveConfig =
        determineEffectiveConfig(config, analysis, strategy.getStrategyName());

    // Perform chunking
    List<Document> chunks = strategy.chunkDocument(content, filename, title, effectiveConfig);

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "Successfully chunked document '{}' into {} chunks using '{}' strategy in {}ms",
        filename,
        chunks.size(),
        strategy.getStrategyName(),
        duration);

    // Add additional metadata to chunks
    enrichChunkMetadata(chunks, analysis, strategy, effectiveConfig, duration);

    return chunks;
  }

  /**
   * Gets information about all available chunking strategies.
   *
   * @return list of strategy information
   */
  public List<ChunkingStrategyRegistry.StrategyInfo> getAvailableStrategies() {
    return strategyRegistry.getStrategyInfo();
  }

  /**
   * Analyzes a document without chunking it.
   *
   * @param content the document content to analyze
   * @return document analysis results
   */
  public DocumentAnalysis analyzeDocument(String content) {
    return documentAnalysisService.analyzeDocument(content);
  }

  /**
   * Gets the recommended strategy for a given document analysis.
   *
   * @param analysis document analysis results
   * @return recommended strategy name
   */
  public String getRecommendedStrategy(DocumentAnalysis analysis) {
    ChunkingStrategy strategy = strategyRegistry.findBestStrategy(analysis);
    return strategy.getStrategyName();
  }

  /**
   * Gets the default configuration for a specific document type and strategy.
   *
   * @param documentType the document type
   * @param strategyName the strategy name (null for default)
   * @return chunking configuration
   */
  public ChunkingConfig getDefaultConfig(
      DocumentAnalysis.DocumentType documentType, String strategyName) {
    return chunkingProperties.getConfigForDocumentType(documentType, strategyName);
  }

  /**
   * Validates that a strategy exists and can handle the given document.
   *
   * @param strategyName strategy name to validate
   * @param analysis document analysis results
   * @return true if strategy is valid for the document
   */
  public boolean isValidStrategy(String strategyName, DocumentAnalysis analysis) {
    try {
      ChunkingStrategy strategy = strategyRegistry.getStrategy(strategyName);
      return strategy.canHandle(analysis);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Chunks multiple documents with the same strategy and configuration. This is more efficient when
   * processing batches of similar documents.
   *
   * @param documents map of filename to content
   * @param titles map of filename to title (optional)
   * @param strategyName strategy to use for all documents
   * @param config configuration to use for all documents
   * @return map of filename to chunks
   */
  public Map<String, List<Document>> chunkDocuments(
      Map<String, String> documents,
      Map<String, String> titles,
      String strategyName,
      ChunkingConfig config) {
    log.info("Batch chunking {} documents with strategy: {}", documents.size(), strategyName);

    Map<String, List<Document>> results = new java.util.HashMap<>();
    long startTime = System.currentTimeMillis();

    for (Map.Entry<String, String> entry : documents.entrySet()) {
      String filename = entry.getKey();
      String content = entry.getValue();
      String title = titles != null ? titles.get(filename) : filename;

      List<Document> chunks = chunkDocument(content, filename, title, strategyName, config);
      results.put(filename, chunks);
    }

    long duration = System.currentTimeMillis() - startTime;
    int totalChunks = results.values().stream().mapToInt(List::size).sum();

    log.info(
        "Batch chunking completed: {} documents, {} total chunks in {}ms",
        documents.size(),
        totalChunks,
        duration);

    return results;
  }

  /** Determines the effective configuration to use based on various inputs. */
  private ChunkingConfig determineEffectiveConfig(
      ChunkingConfig explicitConfig, DocumentAnalysis analysis, String strategyName) {
    if (explicitConfig != null) {
      return explicitConfig;
    }

    // Try to get configuration from properties
    ChunkingConfig propertiesConfig =
        chunkingProperties.getConfigForDocumentType(analysis.getDocumentType(), strategyName);

    if (propertiesConfig != null) {
      return propertiesConfig;
    }

    // Fall back to document-type specific defaults
    return getDocumentTypeDefaultConfig(analysis.getDocumentType());
  }

  /** Gets default configuration based on document type. */
  private ChunkingConfig getDocumentTypeDefaultConfig(DocumentAnalysis.DocumentType documentType) {
    switch (documentType) {
      case TECHNICAL_GUIDE:
      case CODE_HEAVY:
      case API_DOCUMENTATION:
        return ChunkingConfig.technicalConfig();

      case COMPREHENSIVE_DOC:
        return ChunkingConfig.largeChunkConfig();

      case SIMPLE_TEXT:
        return ChunkingConfig.simpleConfig();

      default:
        return ChunkingConfig.defaultConfig();
    }
  }

  /** Enriches chunk metadata with additional information about the chunking process. */
  private void enrichChunkMetadata(
      List<Document> chunks,
      DocumentAnalysis analysis,
      ChunkingStrategy strategy,
      ChunkingConfig config,
      long duration) {
    for (Document chunk : chunks) {
      Map<String, Object> metadata = chunk.getMetadata();

      // Add analysis information (safely handle nulls)
      MetadataUtil.safeMetadataPut(
          metadata,
          "document_type",
          analysis != null && analysis.getDocumentType() != null
              ? analysis.getDocumentType().name()
              : "UNKNOWN");
      MetadataUtil.safeMetadataPut(
          metadata, "document_complexity", analysis != null ? analysis.getComplexityScore() : 0);
      MetadataUtil.safeMetadataPut(
          metadata, "original_document_length", analysis != null ? analysis.getTotalLength() : 0);

      // Add processing information
      MetadataUtil.safeMetadataPut(
          metadata, "chunking_strategy", strategy != null ? strategy.getStrategyName() : "unknown");
      MetadataUtil.safeMetadataPut(metadata, "processing_time_ms", duration);

      // Add configuration used
      MetadataUtil.safeMetadataPut(
          metadata, "chunk_config_max_size", config != null ? config.getMaxChunkSize() : 0);
      MetadataUtil.safeMetadataPut(
          metadata, "chunk_config_overlap", config != null ? config.getOverlapSize() : 0);
    }
  }
}
