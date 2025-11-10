package com.zalando.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

  private Long documentId;
  private String filename;
  private String message;
  private boolean success;
  private String status;
}
