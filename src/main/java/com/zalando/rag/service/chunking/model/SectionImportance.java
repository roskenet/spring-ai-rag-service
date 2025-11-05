package com.zalando.rag.service.chunking.model;

/**
 * Enumeration representing the importance level of a document section. Used to prioritize sections
 * during chunking and retrieval.
 */
public enum SectionImportance {
  HIGH(3, "High importance - key concepts, decisions, conclusions"),
  MEDIUM(2, "Medium importance - supporting information, examples"),
  LOW(1, "Low importance - background, ancillary information");

  private final int priority;
  private final String description;

  SectionImportance(int priority, String description) {
    this.priority = priority;
    this.description = description;
  }

  public int getPriority() {
    return priority;
  }

  public String getDescription() {
    return description;
  }
}
