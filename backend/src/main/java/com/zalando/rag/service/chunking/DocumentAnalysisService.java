package com.zalando.rag.service.chunking;

import com.zalando.rag.service.chunking.DocumentAnalysis.DocumentType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for analyzing document content to determine characteristics that help select the best
 * chunking strategy and parameters.
 */
@Service
@Slf4j
public class DocumentAnalysisService {

  // Patterns for document analysis (extracted from IntelligentChunkingService)
  private static final Pattern HEADER_PATTERN =
      Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern CODE_BLOCK_PATTERN =
      Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
  private static final Pattern LIST_ITEM_PATTERN =
      Pattern.compile("^\\s*[-*+]\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern NUMBERED_LIST_PATTERN =
      Pattern.compile("^\\s*\\d+\\.\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern TABLE_PATTERN = Pattern.compile("\\|.*\\|", Pattern.MULTILINE);
  private static final Pattern MATH_PATTERN =
      Pattern.compile("\\$\\$[\\s\\S]*?\\$\\$|\\$[^$]+\\$", Pattern.MULTILINE);
  private static final Pattern API_ENDPOINT_PATTERN =
      Pattern.compile("(GET|POST|PUT|DELETE|PATCH)\\s+/\\S+", Pattern.MULTILINE);
  private static final Pattern SECTION_BREAK_PATTERN =
      Pattern.compile("\\n\\s*\\n", Pattern.MULTILINE);

  /**
   * Analyzes a document to determine its characteristics and optimal chunking parameters.
   *
   * @param content the document content to analyze
   * @return analysis results
   */
  public DocumentAnalysis analyzeDocument(String content) {
    log.debug("Starting document analysis for content of length: {}", content.length());

    DocumentAnalysis analysis = new DocumentAnalysis();

    // Basic metrics
    analysis.setTotalLength(content.length());
    analysis.setLineCount(content.split("\n").length);
    analysis.setHeaderCount(countMatches(HEADER_PATTERN, content));
    analysis.setCodeBlockCount(countMatches(CODE_BLOCK_PATTERN, content));
    analysis.setListItemCount(
        countMatches(LIST_ITEM_PATTERN, content) + countMatches(NUMBERED_LIST_PATTERN, content));

    // Advanced analysis
    analysis.setContainsTables(containsPattern(TABLE_PATTERN, content));
    analysis.setContainsMath(containsPattern(MATH_PATTERN, content));

    // Calculate ratios
    analysis.setCodeRatio(calculateCodeRatio(analysis));
    analysis.setStructureRatio(calculateStructureRatio(analysis));
    analysis.setAverageParagraphLength(calculateAverageParagraphLength(content));

    // Determine document type and complexity
    analysis.setDocumentType(determineDocumentType(analysis, content));
    analysis.setComplexityScore(calculateComplexityScore(analysis));
    analysis.setOptimalChunkSize(calculateOptimalChunkSize(analysis));

    log.debug("Document analysis completed: {}", analysis);
    return analysis;
  }

  /** Determines the document type based on analysis metrics. */
  private DocumentType determineDocumentType(DocumentAnalysis analysis, String content) {
    // Check for API documentation
    if (containsPattern(API_ENDPOINT_PATTERN, content)
        || content.toLowerCase().contains("endpoint")
        || content.toLowerCase().contains("api reference")) {
      return DocumentType.API_DOCUMENTATION;
    }

    // Check for code-heavy content
    if (analysis.getCodeRatio() > 0.4) {
      return DocumentType.CODE_HEAVY;
    }

    // Check for technical guides
    if (analysis.getCodeRatio() > 0.2 && analysis.getHeaderCount() > 3) {
      return DocumentType.TECHNICAL_GUIDE;
    }

    // Check for tutorials (step-by-step content)
    if (analysis.getListItemCount() > 10
        && (content.toLowerCase().contains("step") || content.toLowerCase().contains("tutorial"))) {
      return DocumentType.TUTORIAL;
    }

    // Check for specifications
    if (analysis.getStructureRatio() > 0.3 && analysis.getHeaderCount() > 5) {
      return DocumentType.SPECIFICATION;
    }

    // Check for comprehensive documents
    if (analysis.getHeaderCount() > 10 && analysis.getTotalLength() > 5000) {
      return DocumentType.COMPREHENSIVE_DOC;
    }

    // Check for simple text
    if (analysis.getHeaderCount() < 3
        && analysis.getListItemCount() < 5
        && analysis.getCodeBlockCount() == 0) {
      return DocumentType.SIMPLE_TEXT;
    }

    return DocumentType.GENERAL_DOC;
  }

  /** Calculates a complexity score (0-100) based on various document characteristics. */
  private int calculateComplexityScore(DocumentAnalysis analysis) {
    int score = 0;

    // Length complexity (0-20 points)
    if (analysis.getTotalLength() > 10000) score += 20;
    else if (analysis.getTotalLength() > 5000) score += 15;
    else if (analysis.getTotalLength() > 2000) score += 10;
    else score += 5;

    // Structure complexity (0-30 points)
    score += Math.min(30, analysis.getHeaderCount() * 2);
    score += Math.min(10, analysis.getListItemCount());

    // Code complexity (0-25 points)
    score += Math.min(25, (int) (analysis.getCodeRatio() * 50));

    // Content complexity (0-25 points)
    if (analysis.isContainsTables()) score += 10;
    if (analysis.isContainsMath()) score += 15;

    return Math.min(100, score);
  }

  /** Calculates optimal chunk size based on document characteristics. */
  private int calculateOptimalChunkSize(DocumentAnalysis analysis) {
    int baseSize = 800; // Default preferred size

    switch (analysis.getDocumentType()) {
      case TECHNICAL_GUIDE:
      case CODE_HEAVY:
        // Larger chunks to keep code with explanations
        return Math.min(2500, baseSize + 600);

      case API_DOCUMENTATION:
        // Medium-large chunks for API examples
        return Math.min(2000, baseSize + 400);

      case SPECIFICATION:
      case TUTORIAL:
        // Medium chunks to keep related items together
        return baseSize;

      case COMPREHENSIVE_DOC:
        // Smaller chunks due to dense information
        return Math.max(500, baseSize - 300);

      case SIMPLE_TEXT:
        // Smaller chunks for simple content
        return Math.max(400, baseSize - 400);

      default:
        return baseSize;
    }
  }

  private double calculateCodeRatio(DocumentAnalysis analysis) {
    if (analysis.getTotalLength() == 0) return 0.0;
    // Rough estimation: each code block represents ~100 characters on average
    double estimatedCodeLength = analysis.getCodeBlockCount() * 100.0;
    return Math.min(1.0, estimatedCodeLength / analysis.getTotalLength());
  }

  private double calculateStructureRatio(DocumentAnalysis analysis) {
    if (analysis.getLineCount() == 0) return 0.0;
    int structuralElements = analysis.getHeaderCount() + analysis.getListItemCount();
    return Math.min(1.0, (double) structuralElements / analysis.getLineCount());
  }

  private double calculateAverageParagraphLength(String content) {
    String[] paragraphs = SECTION_BREAK_PATTERN.split(content);
    if (paragraphs.length == 0) return 0.0;

    double totalLength = 0;
    int validParagraphs = 0;

    for (String paragraph : paragraphs) {
      String trimmed = paragraph.trim();
      if (!trimmed.isEmpty()) {
        totalLength += trimmed.length();
        validParagraphs++;
      }
    }

    return validParagraphs > 0 ? totalLength / validParagraphs : 0.0;
  }

  private int countMatches(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  private boolean containsPattern(Pattern pattern, String text) {
    return pattern.matcher(text).find();
  }
}
