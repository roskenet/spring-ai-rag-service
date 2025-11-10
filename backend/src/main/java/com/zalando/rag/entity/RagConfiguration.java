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
@Table(name = "rag_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagConfiguration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "config_key", nullable = false, unique = true)
  private String configKey;

  @Column(name = "embeddings_model")
  private String embeddingsModel;

  @Column(name = "chunking_strategy")
  private String chunkingStrategy;

  @Column(name = "chunk_size")
  private Integer chunkSize;

  @Column(name = "overlap_percentage")
  private Integer overlapPercentage;

  @Column(name = "similarity_threshold")
  private Double similarityThreshold;

  @Column(name = "max_results")
  private Integer maxResults;

  @Column(name = "include_citations")
  private Boolean includeCitations;

  @Column(name = "temperature")
  private Double temperature;

  @Column(name = "top_k")
  private Integer topK;

  @Column(name = "selected_model")
  private String selectedModel;

  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
