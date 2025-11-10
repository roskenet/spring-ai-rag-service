package com.zalando.rag.service.chunking.model;

/** Represents a section of a document with metadata about its structure and importance. */
public class DocumentSection {
  private String title;
  private String content;
  private int level;
  private SectionImportance importance;

  public DocumentSection(String title, String content, int level, SectionImportance importance) {
    this.title = title;
    this.content = content;
    this.level = level;
    this.importance = importance;
  }

  // Getters and setters
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public SectionImportance getImportance() {
    return importance;
  }

  public void setImportance(SectionImportance importance) {
    this.importance = importance;
  }

  @Override
  public String toString() {
    return String.format(
        "DocumentSection{title='%s', level=%d, importance=%s, contentLength=%d}",
        title, level, importance, content != null ? content.length() : 0);
  }
}
