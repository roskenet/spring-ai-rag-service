package com.zalando.rag.service.chunking;

import com.zalando.rag.service.chunking.util.MetadataUtil;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Simple fixed-size chunking strategy that splits documents into chunks of approximately equal
 * size. This strategy is fast and predictable, making it suitable for simple use cases or when
 * processing large volumes of documents.
 *
 * <p>Similar to AWS Bedrock's default chunking approach, this strategy: - Splits text at character
 * boundaries with configurable overlap - Optionally respects word boundaries to avoid breaking
 * words - Provides consistent chunk sizes for predictable processing
 */
@Component
@Slf4j
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

  @Override
  public String getStrategyName() {
    return "fixed-size";
  }

  @Override
  public String getDescription() {
    return "Simple fixed-size chunking with configurable overlap, suitable for consistent chunk sizes";
  }

  @Override
  public int getPriority() {
    return 50; // Medium priority - good fallback strategy
  }

  @Override
  public boolean canHandle(DocumentAnalysis analysis) {
    // This strategy can handle any document type
    return true;
  }

  @Override
  public List<Document> chunkDocument(
      String content, String filename, String title, ChunkingConfig config) {
    log.info(
        "Starting fixed-size chunking for document: {} with chunk size: {}",
        filename,
        config.getPreferredChunkSize());

    if (content == null || content.trim().isEmpty()) {
      log.warn("Empty content provided for document: {}", filename);
      return Collections.emptyList();
    }

    List<Document> chunks = new ArrayList<>();
    String cleanContent = content.trim();

    // If content is smaller than chunk size, return as single chunk
    if (cleanContent.length() <= config.getMaxChunkSize()) {
      chunks.add(createChunk(cleanContent, filename, title, 0, config));
      log.info("Document {} fits in single chunk of size: {}", filename, cleanContent.length());
      return chunks;
    }

    // Split into fixed-size chunks with overlap
    int chunkIndex = 0;
    int position = 0;
    int chunkSize = config.getPreferredChunkSize();
    int overlapSize = config.getOverlapSize();

    while (position < cleanContent.length()) {
      int endPosition = Math.min(position + chunkSize, cleanContent.length());

      // Extract chunk content
      String chunkContent = cleanContent.substring(position, endPosition);

      // Adjust chunk boundaries to avoid breaking words if configured
      if (config.isMaintainSentenceBoundaries() && endPosition < cleanContent.length()) {
        chunkContent = adjustChunkBoundary(chunkContent, cleanContent, position, endPosition);
        endPosition = position + chunkContent.length();
      }

      // Create and add chunk
      if (!chunkContent.trim().isEmpty()) {
        chunks.add(createChunk(chunkContent, filename, title, chunkIndex++, config));
      }

      // Calculate next position with overlap
      position = Math.max(position + 1, endPosition - overlapSize);

      // Prevent infinite loop
      if (position >= cleanContent.length()) {
        break;
      }
    }

    log.info("Created {} fixed-size chunks for document: {}", chunks.size(), filename);
    return chunks;
  }

  /** Adjusts chunk boundaries to avoid breaking words or sentences when possible. */
  private String adjustChunkBoundary(
      String chunkContent, String fullContent, int startPos, int endPos) {
    // If we're at the end of the document, return as-is
    if (endPos >= fullContent.length()) {
      return chunkContent;
    }

    // Try to end at a sentence boundary
    int lastSentenceEnd = findLastSentenceEnd(chunkContent);
    if (lastSentenceEnd > chunkContent.length() / 2) { // Only if we don't lose too much content
      return chunkContent.substring(0, lastSentenceEnd + 1);
    }

    // Try to end at a word boundary
    int lastWordEnd = findLastWordBoundary(chunkContent);
    if (lastWordEnd > chunkContent.length() / 2) { // Only if we don't lose too much content
      return chunkContent.substring(0, lastWordEnd);
    }

    // Try to end at a whitespace
    int lastWhitespace = findLastWhitespace(chunkContent);
    if (lastWhitespace > chunkContent.length() / 2) {
      return chunkContent.substring(0, lastWhitespace);
    }

    // If no good boundary found, return original chunk
    return chunkContent;
  }

  /** Finds the last sentence-ending punctuation in the chunk. */
  private int findLastSentenceEnd(String text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      char c = text.charAt(i);
      if (c == '.' || c == '!' || c == '?') {
        // Make sure it's not an abbreviation or decimal number
        if (i == text.length() - 1
            || (i < text.length() - 1 && Character.isWhitespace(text.charAt(i + 1)))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Finds the last word boundary in the chunk. */
  private int findLastWordBoundary(String text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      char c = text.charAt(i);
      if (Character.isWhitespace(c) || c == ',' || c == ';' || c == ':') {
        return i;
      }
    }
    return -1;
  }

  /** Finds the last whitespace character in the chunk. */
  private int findLastWhitespace(String text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      if (Character.isWhitespace(text.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  /** Creates a document chunk with appropriate metadata. */
  private Document createChunk(
      String content, String filename, String title, int chunkIndex, ChunkingConfig config) {
    Map<String, Object> metadata = new HashMap<>();

    // Basic metadata
    MetadataUtil.safeMetadataPut(metadata, "filename", filename);
    MetadataUtil.safeMetadataPut(metadata, "title", title);
    MetadataUtil.safeMetadataPut(metadata, "chunk_index", chunkIndex);
    MetadataUtil.safeMetadataPut(metadata, "chunk_size", content.length());
    MetadataUtil.safeMetadataPut(metadata, "chunking_strategy", getStrategyName());

    // Strategy-specific metadata
    MetadataUtil.safeMetadataPut(metadata, "chunk_method", "fixed-size");
    MetadataUtil.safeMetadataPut(metadata, "target_chunk_size", config.getPreferredChunkSize());
    MetadataUtil.safeMetadataPut(metadata, "overlap_size", config.getOverlapSize());
    MetadataUtil.safeMetadataPut(
        metadata, "maintains_word_boundaries", config.isMaintainSentenceBoundaries());

    return new Document(content.trim(), metadata);
  }
}
