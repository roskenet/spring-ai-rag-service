package com.zalando.rag.service.chunking.model;

/** Information about a header found in a document. */
public class HeaderInfo {
  private int level;
  private String text;
  private int position;

  public HeaderInfo(int level, String text, int position) {
    this.level = level;
    this.text = text;
    this.position = position;
  }

  // Getters and setters
  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  @Override
  public String toString() {
    return String.format("HeaderInfo{level=%d, text='%s', position=%d}", level, text, position);
  }
}
