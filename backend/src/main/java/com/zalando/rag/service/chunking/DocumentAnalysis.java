package com.zalando.rag.service.chunking;

import lombok.Data;

/**
 * Analysis results for a document, used to determine the best chunking strategy and optimal
 * parameters.
 */
@Data
public class DocumentAnalysis {

  /** Total document length in characters. */
  private int totalLength;

  /** Number of lines in the document. */
  private int lineCount;

  /** Number of markdown headers found. */
  private int headerCount;

  /** Number of code blocks found. */
  private int codeBlockCount;

  /** Number of list items found. */
  private int listItemCount;

  /** Detected document type based on analysis. */
  private DocumentType documentType;

  /** Recommended chunk size based on document characteristics. */
  private int optimalChunkSize;

  /** Ratio of code content to total content. */
  private double codeRatio;

  /** Ratio of structured content (lists, headers) to total content. */
  private double structureRatio;

  /** Average paragraph length. */
  private double averageParagraphLength;

  /** Complexity score (0-100) based on various factors. */
  private int complexityScore;

  /** Whether the document contains tables. */
  private boolean containsTables;

  /** Whether the document contains mathematical formulas. */
  private boolean containsMath;

  /** Detected language (if applicable). */
  private String language;

  @Override
  public String toString() {
    return String.format(
        "DocumentAnalysis{type=%s, length=%d, headers=%d, code=%d, complexity=%d, optimalChunk=%d}",
        documentType, totalLength, headerCount, codeBlockCount, complexityScore, optimalChunkSize);
  }

  /** Document types based on content analysis. */
  public enum DocumentType {
    TECHNICAL_GUIDE("Technical documentation with code examples"),
    SPECIFICATION("Formal specification or requirements document"),
    COMPREHENSIVE_DOC("Large document with extensive structure"),
    GENERAL_DOC("General text document"),
    CODE_HEAVY("Document with primarily code content"),
    SIMPLE_TEXT("Plain text without structure"),
    API_DOCUMENTATION("API reference documentation"),
    TUTORIAL("Step-by-step tutorial or guide");

    private final String description;

    DocumentType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
