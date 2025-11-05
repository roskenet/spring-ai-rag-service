package com.zalando.rag.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

  private String answer;
  private String question;
  private List<SourceDocument> sources;
  private long responseTimeMs;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SourceDocument {
    private String filename;
    private String title;
    private String content;
    private double similarity;
    private int chunkIndex;
  }
}
