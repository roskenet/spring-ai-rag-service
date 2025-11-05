package com.zalando.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

  @NotBlank(message = "Question cannot be empty")
  @Size(max = 1000, message = "Question cannot exceed 1000 characters")
  private String question;

  @Builder.Default private int maxResults = 5;

  @Builder.Default private double similarityThreshold = 0.7;

  @Builder.Default private boolean includeSourceInfo = true;
}
