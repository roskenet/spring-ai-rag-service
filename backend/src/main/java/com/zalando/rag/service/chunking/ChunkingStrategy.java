package com.zalando.rag.service.chunking;

import java.util.List;
import org.springframework.ai.document.Document;

/**
 * Strategy interface for different document chunking approaches. Implementations can provide
 * various chunking strategies like intelligent, fixed-size, semantic, or hierarchical chunking.
 */
public interface ChunkingStrategy {

  /**
   * Returns the unique name of this chunking strategy.
   *
   * @return strategy name (e.g., "intelligent", "fixed-size", "semantic")
   */
  String getStrategyName();

  /**
   * Chunks a document into smaller pieces using this strategy.
   *
   * @param content the document content to chunk
   * @param filename the filename for metadata
   * @param title the document title for metadata
   * @param config chunking configuration parameters
   * @return list of document chunks with metadata
   */
  List<Document> chunkDocument(
      String content, String filename, String title, ChunkingConfig config);

  /**
   * Determines if this strategy can handle the given document analysis. Used by the strategy
   * registry to select appropriate strategies.
   *
   * @param analysis the document analysis results
   * @return true if this strategy can handle the document
   */
  boolean canHandle(DocumentAnalysis analysis);

  /**
   * Returns the priority of this strategy. Higher values take precedence when multiple strategies
   * can handle the same document.
   *
   * @return priority value (higher = more preferred)
   */
  int getPriority();

  /**
   * Returns a brief description of what this strategy does.
   *
   * @return strategy description
   */
  default String getDescription() {
    return "Chunking strategy: " + getStrategyName();
  }
}
