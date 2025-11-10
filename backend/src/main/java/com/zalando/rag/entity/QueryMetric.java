package com.zalando.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "query_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryMetric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
  private String queryText;

  @Column(name = "response_time_ms", nullable = false)
  private Long responseTimeMs;

  @Column(name = "accuracy_score")
  private Double accuracyScore;

  @Column(name = "similarity_threshold")
  private Double similarityThreshold;

  @Column(name = "max_results")
  private Integer maxResults;

  @Column(name = "results_found")
  private Integer resultsFound;

  @Column(name = "selected_model")
  private String selectedModel;

  @Column(name = "temperature")
  private Double temperature;

  @Column(name = "success", nullable = false)
  private Boolean success;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "user_session_id")
  private String userSessionId;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
