package com.zalando.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "document_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "document_id", nullable = false)
  private Long documentId;

  @Column(name = "document_name", nullable = false)
  private String documentName;

  @Column(name = "document_type")
  private String documentType;

  @Column(name = "category")
  private String category;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "chunk_count", nullable = false)
  private Integer chunkCount;

  @Column(name = "access_count")
  @Builder.Default
  private Long accessCount = 0L;

  @Column(name = "last_accessed")
  private LocalDateTime lastAccessed;

  @Column(name = "processing_time_ms")
  private Long processingTimeMs;

  @Column(name = "chunking_strategy")
  private String chunkingStrategy;

  @Column(name = "embedding_model")
  private String embeddingModel;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
