package com.zalando.rag.service.chunking.model;

/** Represents a code block found in a document, used for preservation during chunking. */
public class CodeBlock {
  private final String content;
  private final int start;
  private final int end;

  public CodeBlock(String content, int start, int end) {
    this.content = content;
    this.start = start;
    this.end = end;
  }

  public String getContent() {
    return content;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  @Override
  public String toString() {
    return String.format("CodeBlock{start=%d, end=%d, length=%d}", start, end, content.length());
  }
}
