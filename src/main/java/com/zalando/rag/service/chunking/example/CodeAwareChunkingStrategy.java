package com.zalando.rag.service.chunking.example;

import com.zalando.rag.service.chunking.ChunkingConfig;
import com.zalando.rag.service.chunking.ChunkingStrategy;
import com.zalando.rag.service.chunking.DocumentAnalysis;
import com.zalando.rag.service.chunking.util.MetadataUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Example custom chunking strategy that is specifically designed for code-heavy documents. This
 * strategy treats code blocks as high-priority content and ensures they are preserved with their
 * surrounding context.
 *
 * <p>This is a practical example of how to implement a custom chunking strategy.
 */
@Component
@Slf4j
public class CodeAwareChunkingStrategy implements ChunkingStrategy {

  // Patterns for detecting different types of code content
  private static final Pattern CODE_BLOCK_PATTERN =
      Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
  private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`[^`]+`");
  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile(
          "(public|private|protected)?\\s*(static)?\\s*(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{",
          Pattern.MULTILINE);
  private static final Pattern CLASS_PATTERN =
      Pattern.compile("(public|private)?\\s*class\\s+(\\w+)", Pattern.MULTILINE);
  private static final Pattern IMPORT_PATTERN =
      Pattern.compile("^import\\s+[\\w.]+;?$", Pattern.MULTILINE);

  @Override
  public String getStrategyName() {
    return "code-aware";
  }

  @Override
  public String getDescription() {
    return "Code-aware strategy that preserves code blocks with their context and handles programming constructs intelligently";
  }

  @Override
  public int getPriority() {
    return 90; // High priority for code-heavy content
  }

  @Override
  public boolean canHandle(DocumentAnalysis analysis) {
    // This strategy is best for documents with significant code content
    return analysis.getDocumentType() == DocumentAnalysis.DocumentType.CODE_HEAVY
        || analysis.getDocumentType() == DocumentAnalysis.DocumentType.TECHNICAL_GUIDE
        || analysis.getCodeRatio() > 0.3
        || analysis.getCodeBlockCount() > 2;
  }

  @Override
  public List<Document> chunkDocument(
      String content, String filename, String title, ChunkingConfig config) {
    log.info("Starting code-aware chunking for document: {}", filename);

    if (content == null || content.trim().isEmpty()) {
      return Collections.emptyList();
    }

    List<CodeSection> codeSections = identifyCodeSections(content);
    List<Document> chunks = createCodeAwareChunks(codeSections, content, filename, title, config);

    log.info("Created {} code-aware chunks for document: {}", chunks.size(), filename);
    return chunks;
  }

  /** Identifies different types of code sections in the document. */
  private List<CodeSection> identifyCodeSections(String content) {
    List<CodeSection> sections = new ArrayList<>();

    // Find code blocks first (highest priority)
    Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(content);
    while (codeBlockMatcher.find()) {
      String codeBlock = codeBlockMatcher.group();
      sections.add(
          new CodeSection(
              CodeSectionType.CODE_BLOCK,
              codeBlockMatcher.start(),
              codeBlockMatcher.end(),
              codeBlock,
              extractLanguage(codeBlock)));
    }

    // Find function definitions
    Matcher functionMatcher = FUNCTION_PATTERN.matcher(content);
    while (functionMatcher.find()) {
      // Only add if not already covered by a code block
      int start = functionMatcher.start();
      if (sections.stream().noneMatch(s -> s.contains(start))) {
        sections.add(
            new CodeSection(
                CodeSectionType.FUNCTION,
                start,
                functionMatcher.end(),
                functionMatcher.group(),
                "java" // Default assumption
                ));
      }
    }

    // Find class definitions
    Matcher classMatcher = CLASS_PATTERN.matcher(content);
    while (classMatcher.find()) {
      int start = classMatcher.start();
      if (sections.stream().noneMatch(s -> s.contains(start))) {
        sections.add(
            new CodeSection(
                CodeSectionType.CLASS, start, classMatcher.end(), classMatcher.group(), "java"));
      }
    }

    // Sort by start position
    sections.sort(Comparator.comparing(CodeSection::getStart));
    return sections;
  }

  /** Creates chunks that preserve code context. */
  private List<Document> createCodeAwareChunks(
      List<CodeSection> codeSections,
      String content,
      String filename,
      String title,
      ChunkingConfig config) {
    List<Document> chunks = new ArrayList<>();

    if (codeSections.isEmpty()) {
      // No code sections, fall back to simple chunking
      return createSimpleChunks(content, filename, title, config);
    }

    int currentPos = 0;
    int chunkIndex = 0;

    for (CodeSection codeSection : codeSections) {
      // Add text before code section if significant
      if (codeSection.getStart() > currentPos) {
        String beforeText = content.substring(currentPos, codeSection.getStart()).trim();
        if (beforeText.length() > config.getMinChunkSize()) {
          chunks.add(
              createChunk(beforeText, filename, title, chunkIndex++, "Text Section", config));
        }
      }

      // Create chunk with code section and its context - ALWAYS keep code blocks together
      String codeChunk = createCodeChunkWithContext(content, codeSection, config);
      chunks.add(
          createChunk(
              codeChunk,
              filename,
              title,
              chunkIndex++,
              "Code Section: " + codeSection.getType().name(),
              config));

      currentPos = codeSection.getEnd();
    }

    // Add remaining content
    if (currentPos < content.length()) {
      String remainingText = content.substring(currentPos).trim();
      if (remainingText.length() > config.getMinChunkSize()) {
        chunks.add(
            createChunk(remainingText, filename, title, chunkIndex, "Final Section", config));
      }
    }

    return chunks;
  }

  /** Creates a chunk that includes the code section with appropriate context. */
  private String createCodeChunkWithContext(
      String content, CodeSection codeSection, ChunkingConfig config) {
    int contextBefore = config.getOverlapSize();
    int contextAfter = config.getOverlapSize();

    // For code blocks, always preserve the entire block
    if (codeSection.getType() == CodeSectionType.CODE_BLOCK) {
      // Find sentence boundaries around the code block for better context
      int start = Math.max(0, codeSection.getStart() - contextBefore);
      int end = Math.min(content.length(), codeSection.getEnd() + contextAfter);

      start = findSentenceStart(content, start, codeSection.getStart());
      end = findSentenceEnd(content, end, codeSection.getEnd());

      String chunkContent = content.substring(start, end);

      // If chunk is too large, reduce context but NEVER split the code block
      if (chunkContent.length() > config.getMaxChunkSize()) {
        int codeBlockLength = codeSection.getEnd() - codeSection.getStart();
        int availableContext = config.getMaxChunkSize() - codeBlockLength;

        if (availableContext > 100) { // Minimum context
          int contextEach = availableContext / 2;
          start = Math.max(0, codeSection.getStart() - contextEach);
          end = Math.min(content.length(), codeSection.getEnd() + contextEach);
          start = findSentenceStart(content, start, codeSection.getStart());
          end = findSentenceEnd(content, end, codeSection.getEnd());
          chunkContent = content.substring(start, end);
        } else {
          // Just use the code block itself, even if it exceeds max size
          chunkContent = content.substring(codeSection.getStart(), codeSection.getEnd());
        }
      }

      return chunkContent;
    }

    // For non-code-block sections, use the original logic
    int start = Math.max(0, codeSection.getStart() - contextBefore);
    int end = Math.min(content.length(), codeSection.getEnd() + contextAfter);

    start = findSentenceStart(content, start, codeSection.getStart());
    end = findSentenceEnd(content, end, codeSection.getEnd());

    String chunkContent = content.substring(start, end);

    if (chunkContent.length() > config.getMaxChunkSize()) {
      int codeLength = codeSection.getContent().length();
      int availableContext = config.getMaxChunkSize() - codeLength;

      if (availableContext > 0) {
        int contextEach = availableContext / 2;
        start = Math.max(0, codeSection.getStart() - contextEach);
        end = Math.min(content.length(), codeSection.getEnd() + contextEach);
        chunkContent = content.substring(start, end);
      } else {
        chunkContent = codeSection.getContent();
      }
    }

    return chunkContent;
  }

  private int findSentenceStart(String content, int position, int limit) {
    for (int i = position; i < limit; i++) {
      char c = content.charAt(i);
      if (c == '.' || c == '!' || c == '?') {
        // Look for space after punctuation
        if (i + 1 < content.length() && Character.isWhitespace(content.charAt(i + 1))) {
          return i + 1;
        }
      }
    }
    return position;
  }

  private int findSentenceEnd(String content, int position, int limit) {
    for (int i = Math.min(position, content.length() - 1); i >= limit; i--) {
      char c = content.charAt(i);
      if (c == '.' || c == '!' || c == '?') {
        return i + 1;
      }
    }
    return position;
  }

  private List<Document> createSimpleChunks(
      String content, String filename, String title, ChunkingConfig config) {
    List<Document> chunks = new ArrayList<>();
    int chunkSize = config.getPreferredChunkSize();
    int overlap = config.getOverlapSize();

    for (int i = 0; i < content.length(); i += chunkSize - overlap) {
      int end = Math.min(i + chunkSize, content.length());
      String chunkContent = content.substring(i, end);

      chunks.add(createChunk(chunkContent, filename, title, chunks.size(), "Simple Chunk", config));

      if (end >= content.length()) break;
    }

    return chunks;
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

    // Add code-specific metadata
    MetadataUtil.safeMetadataPut(metadata, "contains_code_block", content.contains("```"));
    MetadataUtil.safeMetadataPut(
        metadata, "contains_inline_code", INLINE_CODE_PATTERN.matcher(content).find());
    MetadataUtil.safeMetadataPut(
        metadata, "contains_function", FUNCTION_PATTERN.matcher(content).find());
    MetadataUtil.safeMetadataPut(metadata, "contains_class", CLASS_PATTERN.matcher(content).find());

    // Detect programming language if code is present - use safe put to handle null
    String language = extractLanguage(content);
    MetadataUtil.safeMetadataPut(metadata, "programming_language", language);

    return new Document(content.trim(), metadata);
  }

  private String extractLanguage(String codeContent) {
    // First, look for code blocks in the content
    Matcher codeBlockMatcher = Pattern.compile("```(\\w+)?[\\s\\S]*?```").matcher(codeContent);
    if (codeBlockMatcher.find()) {
      String fullMatch = codeBlockMatcher.group();
      if (fullMatch.startsWith("```")) {
        int newlineIndex = fullMatch.indexOf('\n');
        if (newlineIndex > 3) {
          String language = fullMatch.substring(3, newlineIndex).trim().toLowerCase();
          if (!language.isEmpty()) {
            return language;
          }
        }
      }
    }

    // If no explicit language specified, try to detect from content
    if (codeContent.contains("def ")
        || codeContent.contains("import ") && codeContent.contains("print(")) {
      return "python";
    }
    if (codeContent.contains("function ") || codeContent.contains("console.log")) {
      return "javascript";
    }
    if (codeContent.contains("SELECT ")
        || codeContent.contains("FROM ")
        || codeContent.contains("WHERE ")) {
      return "sql";
    }
    if (codeContent.contains("public class") || codeContent.contains("System.out.println")) {
      return "java";
    }

    return null;
  }

  /** Represents a section of code in the document. */
  private static class CodeSection {
    private final CodeSectionType type;
    private final int start;
    private final int end;
    private final String content;
    private final String language;

    public CodeSection(CodeSectionType type, int start, int end, String content, String language) {
      this.type = type;
      this.start = start;
      this.end = end;
      this.content = content;
      this.language = language;
    }

    public boolean contains(int position) {
      return position >= start && position <= end;
    }

    // Getters
    public CodeSectionType getType() {
      return type;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public String getContent() {
      return content;
    }

    public String getLanguage() {
      return language;
    }
  }

  /** Types of code sections we can identify. */
  private enum CodeSectionType {
    CODE_BLOCK, // Markdown code blocks (```)
    FUNCTION, // Function/method definitions
    CLASS, // Class definitions
    IMPORT // Import statements
  }
}
