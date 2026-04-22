package com.zalando.rag.service.chunking;

import com.zalando.rag.service.chunking.model.*;
import com.zalando.rag.service.chunking.util.MetadataUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Intelligent chunking strategy that analyzes document structure and creates context-aware chunks.
 * This strategy preserves code blocks, maintains sentence boundaries, and considers document
 * hierarchy when splitting content.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntelligentChunkingStrategy implements ChunkingStrategy {

  // Patterns for intelligent chunking (from original IntelligentChunkingService)
  private static final Pattern HEADER_PATTERN =
      Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern CODE_BLOCK_PATTERN =
      Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
  private static final Pattern LIST_ITEM_PATTERN =
      Pattern.compile("^\\s*[-*+]\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern NUMBERED_LIST_PATTERN =
      Pattern.compile("^\\s*\\d+\\.\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern SECTION_BREAK_PATTERN =
      Pattern.compile("\\n\\s*\\n", Pattern.MULTILINE);

  // Sentence boundary patterns for smart chunking
  private static final Pattern SENTENCE_END_PATTERN =
      Pattern.compile(
          "(?<=[.!?])\\s+(?=[A-Z])|"
              + // Period/exclamation/question followed by space and capital letter
              "(?<=[.!?])\\n+|"
              + // Period/exclamation/question followed by newlines
              "(?<=\\.)\\s*\\n\\s*-\\s|"
              + // Period followed by list item
              "(?<=\\.)\\s*\\n\\s*\\d+\\.|"
              + // Period followed by numbered list
              "(?<=\\.)\\s*\\n\\s*#{1,6}\\s" // Period followed by header
          );

  @Override
  public String getStrategyName() {
    return "intelligent";
  }

  @Override
  public String getDescription() {
    return "Intelligent strategy that analyzes document structure and preserves context, code blocks, and semantic boundaries";
  }

  @Override
  public int getPriority() {
    return 100; // Highest priority as the default intelligent strategy
  }

  @Override
  public boolean canHandle(DocumentAnalysis analysis) {
    // This strategy can handle any document, but is especially good for structured content
    return true;
  }

  @Override
  public List<Document> chunkDocument(
      String content, String filename, String title, ChunkingConfig config) {
    log.info("Starting intelligent chunking for document: {} with config: {}", filename, config);

    if (content == null || content.trim().isEmpty()) {
      log.warn("Empty content provided for document: {}", filename);
      return Collections.emptyList();
    }

    List<DocumentSection> sections = extractSections(content);
    List<Document> chunks = createChunksFromSections(sections, filename, title, config);

    log.info(
        "Created {} chunks for document {} using intelligent strategy", chunks.size(), filename);
    return chunks;
  }

  /** Extracts sections from the document content based on headers or paragraph breaks. */
  private List<DocumentSection> extractSections(String content) {
    List<DocumentSection> sections = new ArrayList<>();

    // Split by headers first
    Matcher headerMatcher = HEADER_PATTERN.matcher(content);
    List<HeaderInfo> headers = new ArrayList<>();

    while (headerMatcher.find()) {
      headers.add(
          new HeaderInfo(
              headerMatcher.group(1).length(), // header level
              headerMatcher.group(2).trim(), // header text
              headerMatcher.start() // position
              ));
    }

    if (headers.isEmpty()) {
      // No headers, split by paragraphs/sections
      return extractSectionsByParagraphs(content);
    }

    // Create sections based on headers
    for (int i = 0; i < headers.size(); i++) {
      HeaderInfo current = headers.get(i);
      int startPos = current.getPosition();
      int endPos = (i + 1 < headers.size()) ? headers.get(i + 1).getPosition() : content.length();

      String sectionContent = content.substring(startPos, endPos).trim();

      sections.add(
          new DocumentSection(
              current.getText(),
              sectionContent,
              current.getLevel(),
              determineImportance(sectionContent, current.getLevel())));
    }

    return sections;
  }

  /** Extracts sections by paragraph breaks when no headers are found. */
  private List<DocumentSection> extractSectionsByParagraphs(String content) {
    List<DocumentSection> sections = new ArrayList<>();
    String[] paragraphs = SECTION_BREAK_PATTERN.split(content);

    for (int i = 0; i < paragraphs.length; i++) {
      String paragraph = paragraphs[i].trim();
      if (!paragraph.isEmpty()) {
        sections.add(
            new DocumentSection("Section " + (i + 1), paragraph, 0, SectionImportance.MEDIUM));
      }
    }

    return sections;
  }

  /** Determines the importance of a section based on its content and header level. */
  private SectionImportance determineImportance(String content, int headerLevel) {
    // Higher importance for:
    // - Top level headers (1-2)
    // - Sections with code blocks
    // - Sections with decision/conclusion keywords

    if (headerLevel <= 2) return SectionImportance.HIGH;

    String lowerContent = content.toLowerCase();
    if (lowerContent.contains("decision")
        || lowerContent.contains("conclusion")
        || lowerContent.contains("summary")
        || lowerContent.contains("architecture")
        || CODE_BLOCK_PATTERN.matcher(content).find()) {
      return SectionImportance.HIGH;
    }

    if (headerLevel <= 4) return SectionImportance.MEDIUM;
    return SectionImportance.LOW;
  }

  /** Creates chunks from document sections with intelligent merging and splitting. */
  private List<Document> createChunksFromSections(
      List<DocumentSection> sections, String filename, String title, ChunkingConfig config) {
    List<Document> chunks = new ArrayList<>();

    if (sections.isEmpty()) {
      return chunks;
    }

    // Merge small sections with larger ones to ensure minimum chunk size
    List<MergedSection> mergedSections = mergeSmallSections(sections, config);

    // Create chunks from merged sections
    for (MergedSection mergedSection : mergedSections) {
      List<Document> sectionChunks =
          createSentenceAwareChunks(
              mergedSection.toDocumentSection(), filename, title, chunks.size(), config);
      chunks.addAll(sectionChunks);
    }

    // Ensure we always return at least one chunk for non-empty content
    if (chunks.isEmpty()) {
      // Create a single chunk from all content
      StringBuilder allContent = new StringBuilder();
      for (DocumentSection section : sections) {
        allContent.append(formatSection(section));
      }

      String content = allContent.toString().trim();
      if (!content.isEmpty()) {
        chunks.add(createChunk(content, filename, title, 0, "Complete Document", config));
      }
    }

    return chunks;
  }

  /** Merges small sections to meet minimum chunk size requirements. */
  private List<MergedSection> mergeSmallSections(
      List<DocumentSection> sections, ChunkingConfig config) {
    List<MergedSection> merged = new ArrayList<>();
    MergedSection currentMerged = null;

    for (DocumentSection section : sections) {
      String sectionText = formatSection(section);

      // If this section is large enough on its own, or we have no current merged section
      if (sectionText.length() >= config.getMinChunkSize() || currentMerged == null) {
        // Save any previous merged section
        if (currentMerged != null) {
          merged.add(currentMerged);
        }
        // Start new merged section
        currentMerged = new MergedSection(section, config);
      } else {
        // Try to add to current merged section
        if (currentMerged.canAdd(sectionText)) {
          currentMerged.add(section);
        } else {
          // Current merged section is full, save it and start new one
          merged.add(currentMerged);
          currentMerged = new MergedSection(section, config);
        }
      }
    }

    // Add the last merged section
    if (currentMerged != null) {
      merged.add(currentMerged);
    }

    return merged;
  }

  /** Helper class for merging small sections. */
  private static class MergedSection {
    private final List<DocumentSection> sections = new ArrayList<>();
    private int totalLength = 0;
    private final ChunkingConfig config;

    MergedSection(DocumentSection firstSection, ChunkingConfig config) {
      this.config = config;
      add(firstSection);
    }

    boolean canAdd(String sectionText) {
      return totalLength + sectionText.length() <= config.getMaxChunkSize();
    }

    void add(DocumentSection section) {
      sections.add(section);
      totalLength += formatSection(section).length();
    }

    DocumentSection toDocumentSection() {
      if (sections.size() == 1) {
        return sections.get(0);
      }

      // Merge multiple sections into one
      StringBuilder content = new StringBuilder();
      StringBuilder title = new StringBuilder();
      int minLevel = Integer.MAX_VALUE;
      SectionImportance maxImportance = SectionImportance.LOW;

      for (int i = 0; i < sections.size(); i++) {
        DocumentSection section = sections.get(i);
        if (i > 0) {
          title.append(" + ");
          content.append("\n\n");
        }
        title.append(section.getTitle());
        content.append(section.getContent());
        minLevel = Math.min(minLevel, section.getLevel());
        if (section.getImportance().getPriority() > maxImportance.getPriority()) {
          maxImportance = section.getImportance();
        }
      }

      return new DocumentSection(title.toString(), content.toString(), minLevel, maxImportance);
    }
  }

  /** Creates sentence-aware chunks from a document section. */
  private List<Document> createSentenceAwareChunks(
      DocumentSection section,
      String filename,
      String title,
      int startIndex,
      ChunkingConfig config) {
    List<Document> chunks = new ArrayList<>();
    String sectionText = formatSection(section);

    // If section is small enough, keep it as one chunk
    if (sectionText.length() <= config.getMaxChunkSize()) {
      chunks.add(createChunk(sectionText, filename, title, startIndex, section.getTitle(), config));
      return chunks;
    }

    // Split section into sentences while preserving code blocks
    List<String> sentences = splitIntoSentences(sectionText, config);

    StringBuilder currentChunk = new StringBuilder();
    int chunkIndex = startIndex;

    for (String sentence : sentences) {
      // Check if adding this sentence would exceed chunk size
      if (currentChunk.length() + sentence.length() > config.getMaxChunkSize()
          && currentChunk.length() > 0) {

        // Create chunk from current content
        String chunkContent = currentChunk.toString().trim();
        if (!chunkContent.isEmpty()) {
          chunks.add(
              createChunk(chunkContent, filename, title, chunkIndex++, section.getTitle(), config));
        }

        // Start new chunk with sentence-aware overlap
        String overlap = getSentenceAwareOverlap(currentChunk.toString(), config);
        currentChunk = new StringBuilder(overlap);
      }

      currentChunk.append(sentence);

      // If a single sentence exceeds maxChunkSize (e.g. a large code block),
      // split it into fixed-size pieces to avoid exceeding embedding model token limits
      if (currentChunk.length() > config.getMaxChunkSize()) {
        String chunkContent = currentChunk.toString().trim();
        if (!chunkContent.isEmpty()) {
          int splitSize = config.getMaxChunkSize();
          for (int offset = 0; offset < chunkContent.length(); offset += splitSize) {
            String piece =
                chunkContent
                    .substring(offset, Math.min(offset + splitSize, chunkContent.length()))
                    .trim();
            if (!piece.isEmpty()) {
              chunks.add(
                  createChunk(piece, filename, title, chunkIndex++, section.getTitle(), config));
            }
          }
        }
        currentChunk = new StringBuilder();
      }
    }

    // Add remaining content as final chunk
    if (currentChunk.length() > 0) {
      String chunkContent = currentChunk.toString().trim();
      if (!chunkContent.isEmpty()) {
        chunks.add(
            createChunk(chunkContent, filename, title, chunkIndex, section.getTitle(), config));
      }
    }

    return chunks;
  }

  /** Splits text into sentences while preserving code blocks. */
  private List<String> splitIntoSentences(String text, ChunkingConfig config) {
    if (!config.isMaintainSentenceBoundaries()) {
      // Simple character-based splitting
      List<String> chunks = new ArrayList<>();
      int chunkSize = config.getPreferredChunkSize();
      for (int i = 0; i < text.length(); i += chunkSize) {
        int end = Math.min(i + chunkSize, text.length());
        chunks.add(text.substring(i, end));
      }
      return chunks;
    }

    List<String> sentences = new ArrayList<>();

    // Preserve code blocks if configured
    if (config.isPreserveCodeBlocks()) {
      List<CodeBlock> codeBlocks = extractCodeBlocks(text);
      String textWithPlaceholders = replaceCodeBlocksWithPlaceholders(text, codeBlocks);

      // Split by sentence boundaries
      String[] sentenceParts = SENTENCE_END_PATTERN.split(textWithPlaceholders);

      for (int i = 0; i < sentenceParts.length; i++) {
        String part = sentenceParts[i].trim();
        if (!part.isEmpty()) {
          // Restore code blocks
          String restoredPart = restoreCodeBlocks(part, codeBlocks);

          // Add sentence-ending punctuation back (except for last part)
          if (i < sentenceParts.length - 1) {
            if (restoredPart.endsWith(".")
                || restoredPart.endsWith("!")
                || restoredPart.endsWith("?")) {
              sentences.add(restoredPart + " ");
            } else {
              sentences.add(restoredPart + ". ");
            }
          } else {
            sentences.add(restoredPart);
          }
        }
      }
    } else {
      // Simple sentence splitting without code block preservation
      String[] sentenceParts = SENTENCE_END_PATTERN.split(text);
      for (String part : sentenceParts) {
        if (!part.trim().isEmpty()) {
          sentences.add(part.trim() + " ");
        }
      }
    }

    return sentences;
  }

  // Code block preservation methods (extracted from original service)
  private List<CodeBlock> extractCodeBlocks(String text) {
    List<CodeBlock> codeBlocks = new ArrayList<>();
    Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);

    while (matcher.find()) {
      codeBlocks.add(new CodeBlock(matcher.group(), matcher.start(), matcher.end()));
    }

    return codeBlocks;
  }

  private String replaceCodeBlocksWithPlaceholders(String text, List<CodeBlock> codeBlocks) {
    StringBuilder result = new StringBuilder(text);

    // Replace in reverse order to maintain positions
    for (int i = codeBlocks.size() - 1; i >= 0; i--) {
      CodeBlock block = codeBlocks.get(i);
      String placeholder = "___CODEBLOCK_" + i + "___";
      result.replace(block.getStart(), block.getEnd(), placeholder);
    }

    return result.toString();
  }

  private String restoreCodeBlocks(String text, List<CodeBlock> codeBlocks) {
    String result = text;

    for (int i = 0; i < codeBlocks.size(); i++) {
      String placeholder = "___CODEBLOCK_" + i + "___";
      result = result.replace(placeholder, codeBlocks.get(i).getContent());
    }

    return result;
  }

  private String getSentenceAwareOverlap(String text, ChunkingConfig config) {
    if (text.length() <= config.getOverlapSize()) return text;

    String overlap = text.substring(Math.max(0, text.length() - config.getOverlapSize()));

    // Try to start overlap at sentence boundary if configured
    if (config.isMaintainSentenceBoundaries()) {
      Matcher matcher = SENTENCE_END_PATTERN.matcher(overlap);
      if (matcher.find()) {
        int sentenceStart = matcher.end();
        if (sentenceStart > 0 && sentenceStart < overlap.length()) {
          overlap = overlap.substring(sentenceStart);
        }
      }
    }

    return overlap.trim() + " ";
  }

  private static String formatSection(DocumentSection section) {
    StringBuilder formatted = new StringBuilder();

    if (section.getLevel() > 0) {
      formatted.append("#".repeat(section.getLevel())).append(" ");
    }

    formatted.append(section.getTitle()).append("\n\n");
    formatted.append(section.getContent()).append("\n\n");

    return formatted.toString();
  }

  private Document createChunk(
      String content,
      String filename,
      String title,
      int chunkIndex,
      String sectionTitle,
      ChunkingConfig config) {
    Map<String, Object> metadata = new HashMap<>();
    MetadataUtil.safeMetadataPut(metadata, "filename", filename);
    MetadataUtil.safeMetadataPut(metadata, "title", title);
    MetadataUtil.safeMetadataPut(metadata, "chunk_index", chunkIndex);
    MetadataUtil.safeMetadataPut(metadata, "section_title", sectionTitle);
    MetadataUtil.safeMetadataPut(metadata, "chunk_size", content.length());
    MetadataUtil.safeMetadataPut(metadata, "chunking_strategy", getStrategyName());

    // Add config-specific metadata if enabled
    if (config.isIncludeStructuralMetadata()) {
      MetadataUtil.safeMetadataPut(metadata, "preserve_code_blocks", config.isPreserveCodeBlocks());
      MetadataUtil.safeMetadataPut(
          metadata, "maintain_sentence_boundaries", config.isMaintainSentenceBoundaries());
    }

    return new Document(content.trim(), metadata);
  }
}
