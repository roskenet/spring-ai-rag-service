package com.zalando.rag.service.chunking;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration class for chunking parameters. Provides flexible configuration options that can be
 * used by different chunking strategies.
 */
@Data
@Builder
public class ChunkingConfig {

  /**
   * Minimum chunk size in characters. Chunks smaller than this will be merged with adjacent chunks.
   */
  @Builder.Default private int minChunkSize = 200;

  /** Maximum chunk size in characters. Chunks will not exceed this size. */
  @Builder.Default private int maxChunkSize = 2000;

  /** Preferred chunk size in characters. Strategies will aim for this size when possible. */
  @Builder.Default private int preferredChunkSize = 800;

  /** Overlap size in characters between consecutive chunks to maintain context. */
  @Builder.Default private int overlapSize = 100;

  /** Whether to preserve code blocks as single units without splitting them. */
  @Builder.Default private boolean preserveCodeBlocks = true;

  /** Whether to maintain sentence boundaries when splitting text. */
  @Builder.Default private boolean maintainSentenceBoundaries = true;

  /** Whether to preserve markdown structure (headers, lists, etc.). */
  @Builder.Default private boolean preserveMarkdownStructure = true;

  /** Whether to include metadata about sections and importance. */
  @Builder.Default private boolean includeStructuralMetadata = true;

  /** Custom delimiter for splitting (used by recursive strategies). */
  private String customDelimiter;

  /** Whether to use semantic similarity for chunk boundaries (requires embeddings). */
  @Builder.Default private boolean useSemanticBoundaries = false;

  /** Similarity threshold for semantic chunking (0.0 to 1.0). */
  @Builder.Default private double semanticSimilarityThreshold = 0.7;

  /** Creates a default configuration suitable for most documents. */
  public static ChunkingConfig defaultConfig() {
    return ChunkingConfig.builder().build();
  }

  /** Creates a configuration optimized for technical documents with code. */
  public static ChunkingConfig technicalConfig() {
    return ChunkingConfig.builder()
        .maxChunkSize(2500)
        .preferredChunkSize(1200)
        .preserveCodeBlocks(true)
        .preserveMarkdownStructure(true)
        .build();
  }

  /** Creates a configuration optimized for simple text processing. */
  public static ChunkingConfig simpleConfig() {
    return ChunkingConfig.builder()
        .maxChunkSize(1000)
        .preferredChunkSize(500)
        .maintainSentenceBoundaries(false)
        .preserveMarkdownStructure(false)
        .includeStructuralMetadata(false)
        .build();
  }

  /** Creates a configuration with large chunks for comprehensive documents. */
  public static ChunkingConfig largeChunkConfig() {
    return ChunkingConfig.builder()
        .minChunkSize(500)
        .maxChunkSize(4000)
        .preferredChunkSize(2000)
        .overlapSize(200)
        .build();
  }
}
