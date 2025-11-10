package com.zalando.rag.dto;

import com.zalando.rag.service.chunking.ChunkingConfig;
import lombok.Builder;
import lombok.Data;

/**
 * Options for document processing including chunking strategy selection. This class allows clients
 * to specify how documents should be processed when they are uploaded and chunked for the RAG
 * system.
 */
@Data
@Builder
public class ProcessingOptions {

  /**
   * Specific chunking strategy to use. If null, the system will automatically select the best
   * strategy based on document analysis. Available strategies: "intelligent", "fixed-size",
   * "recursive"
   */
  private String chunkingStrategy;

  /**
   * Custom chunking configuration. If null, default configuration will be used based on document
   * type and selected strategy.
   */
  private ChunkingConfig chunkingConfig;

  /** Whether to include detailed metadata about the chunking process in the results. */
  @Builder.Default private boolean includeProcessingMetadata = true;

  /** Whether to perform document analysis and include the results in chunk metadata. */
  @Builder.Default private boolean includeDocumentAnalysis = true;

  /**
   * Whether to validate the selected strategy can handle the document type. If false and strategy
   * is incompatible, processing may fail.
   */
  @Builder.Default private boolean validateStrategy = true;

  /**
   * Maximum number of chunks to create from a single document. If the document would result in more
   * chunks, it will be split differently or processing may be rejected. 0 means no limit.
   */
  @Builder.Default private int maxChunksPerDocument = 0;

  /**
   * Whether to preserve the original document structure in chunk metadata. This includes section
   * titles, hierarchy levels, etc.
   */
  @Builder.Default private boolean preserveStructure = true;

  /** Custom metadata to add to all chunks created from this document. */
  private java.util.Map<String, Object> customMetadata;

  /**
   * Language hint for the document content. Can be used to optimize chunking for specific
   * languages.
   */
  private String languageHint;

  /**
   * Creates default processing options suitable for most use cases. Note: This uses a hardcoded
   * default. Use defaultOptions(String) to respect configuration.
   */
  public static ProcessingOptions defaultOptions() {
    return ProcessingOptions.builder().chunkingStrategy("intelligent").build();
  }

  /** Creates default processing options using the specified default strategy. */
  public static ProcessingOptions defaultOptions(String defaultStrategy) {
    return ProcessingOptions.builder()
        .chunkingStrategy(defaultStrategy != null ? defaultStrategy : "intelligent")
        .build();
  }

  /** Creates processing options optimized for technical documentation. */
  public static ProcessingOptions technicalDocumentOptions() {
    return ProcessingOptions.builder()
        .chunkingStrategy("intelligent")
        .chunkingConfig(ChunkingConfig.technicalConfig())
        .preserveStructure(true)
        .includeProcessingMetadata(true)
        .build();
  }

  /** Creates processing options optimized for simple text processing. */
  public static ProcessingOptions simpleTextOptions() {
    return ProcessingOptions.builder()
        .chunkingStrategy("fixed-size")
        .chunkingConfig(ChunkingConfig.simpleConfig())
        .preserveStructure(false)
        .includeProcessingMetadata(false)
        .build();
  }

  /** Creates processing options optimized for large documents. */
  public static ProcessingOptions largeDocumentOptions() {
    return ProcessingOptions.builder()
        .chunkingStrategy("recursive")
        .chunkingConfig(ChunkingConfig.largeChunkConfig())
        .preserveStructure(true)
        .maxChunksPerDocument(100) // Prevent excessive chunking
        .build();
  }
}
