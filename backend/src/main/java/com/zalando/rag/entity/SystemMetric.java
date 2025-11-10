package com.zalando.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "system_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "metric_type", nullable = false)
  private String metricType;

  @Column(name = "metric_value", nullable = false)
  private Double metricValue;

  @Column(name = "metric_unit")
  private String metricUnit;

  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata;

  @CreationTimestamp
  @Column(name = "recorded_at", updatable = false)
  private LocalDateTime recordedAt;

  public enum MetricType {
    // Existing metrics
    STORAGE_USAGE_MB,
    MEMORY_USAGE_MB,
    CPU_USAGE_PERCENT,
    ACTIVE_CONNECTIONS,
    ERROR_RATE_PERCENT,
    AVERAGE_RESPONSE_TIME_MS,
    QUERIES_PER_MINUTE,
    DOCUMENTS_PROCESSED_PER_HOUR,
    EMBEDDING_GENERATION_TIME_MS,
    VECTOR_SEARCH_TIME_MS,

    // New basic metrics for tracking
    TOTAL_LATENCY_MS, // End-to-end query latency
    RETRIEVAL_LATENCY_MS, // Time to retrieve documents from vector store
    ERROR_COUNT, // Number of errors
    SUCCESS_COUNT, // Number of successful queries
    INPUT_TOKENS, // Number of input tokens used
    OUTPUT_TOKENS, // Number of output tokens generated
    TOTAL_TOKENS, // Total tokens (input + output)
    QUERY_COST_USD, // Cost per query in USD
    DOCUMENTS_RETRIEVED // Number of documents retrieved per query
  }
}
