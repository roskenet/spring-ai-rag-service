package com.zalando.rag.service.chunking;

import com.zalando.rag.service.chunking.util.MetadataUtil;
import java.util.*;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Recursive chunking strategy that attempts to split text using a hierarchy of delimiters. This
 * strategy is similar to LangChain's RecursiveCharacterTextSplitter and is particularly effective
 * for maintaining semantic coherence while respecting document structure.
 *
 * <p>The strategy works by: 1. Trying to split by paragraph breaks first 2. If chunks are still too
 * large, splitting by sentence endings 3. If still too large, splitting by other delimiters
 * (commas, etc.) 4. Finally falling back to character-based splitting if necessary
 */
@Component
@Slf4j
public class RecursiveChunkingStrategy implements ChunkingStrategy {

  // Default hierarchy of delimiters (in order of preference)
  private static final String[] DEFAULT_DELIMITERS = {
    "\n\n", // Paragraph breaks (highest priority)
    "\n", // Line breaks
    ". ", // Sentence endings
    "! ", // Exclamation endings
    "? ", // Question endings
    "; ", // Semicolons
    ", ", // Commas
    " ", // Spaces
    "" // Character-level (last resort)
  };

  // Regex patterns for different types of splits
  private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n\\s*\n");
  private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");
  private static final Pattern LINE_PATTERN = Pattern.compile("\n");

  @Override
  public String getStrategyName() {
    return "recursive";
  }

  @Override
  public String getDescription() {
    return "Recursive strategy that splits text using a hierarchy of delimiters, preserving semantic boundaries";
  }

  @Override
  public int getPriority() {
    return 75; // High priority - good for structured text
  }

  @Override
  public boolean canHandle(DocumentAnalysis analysis) {
    // This strategy works well for most document types, especially those with structure
    return true;
  }

  @Override
  public List<Document> chunkDocument(
      String content, String filename, String title, ChunkingConfig config) {
    log.info(
        "Starting recursive chunking for document: {} with chunk size: {}",
        filename,
        config.getPreferredChunkSize());

    if (content == null || content.trim().isEmpty()) {
      log.warn("Empty content provided for document: {}", filename);
      return Collections.emptyList();
    }

    String[] delimiters = getDelimiters(config);
    List<String> textChunks = recursiveSplit(content.trim(), delimiters, 0, config);

    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < textChunks.size(); i++) {
      String chunk = textChunks.get(i).trim();
      if (!chunk.isEmpty()) {
        documents.add(createChunk(chunk, filename, title, i, config));
      }
    }

    log.info("Created {} recursive chunks for document: {}", documents.size(), filename);
    return documents;
  }

  /** Gets the delimiter hierarchy to use for splitting. */
  private String[] getDelimiters(ChunkingConfig config) {
    if (config.getCustomDelimiter() != null) {
      // If custom delimiter is specified, use it first
      String[] customDelimiters = new String[DEFAULT_DELIMITERS.length + 1];
      customDelimiters[0] = config.getCustomDelimiter();
      System.arraycopy(DEFAULT_DELIMITERS, 0, customDelimiters, 1, DEFAULT_DELIMITERS.length);
      return customDelimiters;
    }
    return DEFAULT_DELIMITERS;
  }

  /** Recursively splits text using the delimiter hierarchy. */
  private List<String> recursiveSplit(
      String text, String[] delimiters, int delimiterIndex, ChunkingConfig config) {
    List<String> result = new ArrayList<>();

    // If text is small enough, return as-is
    if (text.length() <= config.getMaxChunkSize()) {
      result.add(text);
      return result;
    }

    // If we've exhausted all delimiters, do character-based splitting
    if (delimiterIndex >= delimiters.length) {
      return characterSplit(text, config);
    }

    String delimiter = delimiters[delimiterIndex];
    String[] parts;

    // Handle special cases for empty delimiter (character-level splitting)
    if (delimiter.isEmpty()) {
      return characterSplit(text, config);
    }

    // Split by current delimiter
    if (delimiter.equals("\n\n")) {
      parts = PARAGRAPH_PATTERN.split(text);
    } else if (delimiter.equals(". ") || delimiter.equals("! ") || delimiter.equals("? ")) {
      parts = SENTENCE_PATTERN.split(text);
    } else if (delimiter.equals("\n")) {
      parts = LINE_PATTERN.split(text);
    } else {
      parts = text.split(Pattern.quote(delimiter));
    }

    // If no split occurred, try next delimiter
    if (parts.length <= 1) {
      return recursiveSplit(text, delimiters, delimiterIndex + 1, config);
    }

    // Process each part
    List<String> currentChunk = new ArrayList<>();
    int currentLength = 0;

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];

      // Add delimiter back (except for last part)
      if (i < parts.length - 1 && !delimiter.isEmpty()) {
        part += delimiter;
      }

      // If adding this part would exceed max size, process current chunk
      if (currentLength + part.length() > config.getMaxChunkSize() && !currentChunk.isEmpty()) {
        String chunk = String.join("", currentChunk);
        if (chunk.length() > config.getMaxChunkSize()) {
          // Chunk is still too large, split recursively
          result.addAll(recursiveSplit(chunk, delimiters, delimiterIndex + 1, config));
        } else {
          result.add(chunk);
        }

        // Start new chunk with overlap if configured
        currentChunk.clear();
        currentLength = 0;

        // Add overlap from previous chunk
        if (config.getOverlapSize() > 0 && !result.isEmpty()) {
          String lastChunk = result.get(result.size() - 1);
          String overlap = getOverlap(lastChunk, config.getOverlapSize());
          if (!overlap.isEmpty()) {
            currentChunk.add(overlap);
            currentLength += overlap.length();
          }
        }
      }

      // Add current part to chunk
      currentChunk.add(part);
      currentLength += part.length();
    }

    // Process remaining chunk
    if (!currentChunk.isEmpty()) {
      String chunk = String.join("", currentChunk);
      if (chunk.length() > config.getMaxChunkSize()) {
        // Chunk is still too large, split recursively
        result.addAll(recursiveSplit(chunk, delimiters, delimiterIndex + 1, config));
      } else {
        result.add(chunk);
      }
    }

    return result;
  }

  /** Performs character-based splitting as last resort. */
  private List<String> characterSplit(String text, ChunkingConfig config) {
    List<String> result = new ArrayList<>();
    int chunkSize = config.getPreferredChunkSize();
    int overlapSize = config.getOverlapSize();

    for (int i = 0; i < text.length(); i += chunkSize - overlapSize) {
      int end = Math.min(i + chunkSize, text.length());
      String chunk = text.substring(i, end);

      // Try to end at word boundary if possible
      if (config.isMaintainSentenceBoundaries() && end < text.length()) {
        int lastSpace = chunk.lastIndexOf(' ');
        if (lastSpace > chunk.length() / 2) { // Only if we don't lose too much
          chunk = chunk.substring(0, lastSpace);
        }
      }

      result.add(chunk);

      // Break if we've reached the end
      if (end >= text.length()) {
        break;
      }
    }

    return result;
  }

  /** Gets overlap text from the end of a chunk. */
  private String getOverlap(String text, int overlapSize) {
    if (text.length() <= overlapSize) {
      return text;
    }

    String overlap = text.substring(text.length() - overlapSize);

    // Try to start overlap at word boundary
    int firstSpace = overlap.indexOf(' ');
    if (firstSpace > 0 && firstSpace < overlap.length() / 2) {
      overlap = overlap.substring(firstSpace + 1);
    }

    return overlap;
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
    MetadataUtil.safeMetadataPut(metadata, "chunk_method", "recursive-delimiter");
    MetadataUtil.safeMetadataPut(metadata, "max_chunk_size", config.getMaxChunkSize());
    MetadataUtil.safeMetadataPut(metadata, "overlap_size", config.getOverlapSize());
    MetadataUtil.safeMetadataPut(
        metadata, "maintains_boundaries", config.isMaintainSentenceBoundaries());

    MetadataUtil.safeMetadataPut(metadata, "custom_delimiter", config.getCustomDelimiter());

    return new Document(content, metadata);
  }
}
