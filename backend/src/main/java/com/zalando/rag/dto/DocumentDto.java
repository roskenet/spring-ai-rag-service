package com.zalando.rag.dto;

import com.zalando.rag.entity.Document;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

  private Long id;
  private String filename;
  private String title;
  private Long fileSize;
  private Integer chunkCount;
  private Document.DocumentStatus status;
  private String errorMessage;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static DocumentDto fromEntity(Document document) {
    return DocumentDto.builder()
        .id(document.getId())
        .filename(document.getFilename())
        .title(document.getTitle())
        .fileSize(document.getFileSize())
        .chunkCount(document.getChunkCount())
        .status(document.getStatus())
        .errorMessage(document.getErrorMessage())
        .createdAt(document.getCreatedAt())
        .updatedAt(document.getUpdatedAt())
        .build();
  }
}
