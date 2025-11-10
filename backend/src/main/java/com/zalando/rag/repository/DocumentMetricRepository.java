package com.zalando.rag.repository;

import com.zalando.rag.entity.DocumentMetric;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentMetricRepository extends JpaRepository<DocumentMetric, Long> {

  Optional<DocumentMetric> findByDocumentId(Long documentId);

  // Document growth analytics
  @Query(
      "SELECT DATE(d.createdAt) as date, COUNT(d) as count FROM DocumentMetric d WHERE d.createdAt >= :startTime AND d.createdAt <= :endTime GROUP BY DATE(d.createdAt) ORDER BY date")
  List<Object[]> getDocumentGrowthByDay(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  @Query(
      "SELECT DATE(d.createdAt) as date, SUM(d.chunkCount) as totalChunks FROM DocumentMetric d WHERE d.createdAt >= :startTime AND d.createdAt <= :endTime GROUP BY DATE(d.createdAt) ORDER BY date")
  List<Object[]> getChunkGrowthByDay(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  // Document type distribution
  @Query(
      "SELECT d.category, COUNT(d) as count FROM DocumentMetric d GROUP BY d.category ORDER BY count DESC")
  List<Object[]> getDocumentTypeDistribution();

  @Query(
      "SELECT d.documentType, COUNT(d) as count FROM DocumentMetric d GROUP BY d.documentType ORDER BY count DESC")
  List<Object[]> getFileTypeDistribution();

  // Most accessed documents
  @Query("SELECT d.documentName, d.accessCount FROM DocumentMetric d ORDER BY d.accessCount DESC")
  List<Object[]> getMostAccessedDocuments(org.springframework.data.domain.Pageable pageable);

  // Storage analytics
  @Query("SELECT SUM(d.fileSize) FROM DocumentMetric d")
  Long getTotalStorageUsed();

  @Query("SELECT SUM(d.chunkCount) FROM DocumentMetric d")
  Long getTotalChunkCount();

  @Query("SELECT COUNT(d) FROM DocumentMetric d")
  Long getTotalDocumentCount();

  // Performance analytics
  @Query(
      "SELECT AVG(d.processingTimeMs) FROM DocumentMetric d WHERE d.processingTimeMs IS NOT NULL")
  Double getAverageProcessingTime();

  // Update access count
  @Modifying
  @Query(
      "UPDATE DocumentMetric d SET d.accessCount = d.accessCount + 1, d.lastAccessed = :accessTime WHERE d.documentId = :documentId")
  void incrementAccessCount(
      @Param("documentId") Long documentId, @Param("accessTime") LocalDateTime accessTime);

  // Documents by chunking strategy
  @Query(
      "SELECT d.chunkingStrategy, COUNT(d) as count FROM DocumentMetric d WHERE d.chunkingStrategy IS NOT NULL GROUP BY d.chunkingStrategy ORDER BY count DESC")
  List<Object[]> getChunkingStrategyDistribution();

  // Documents by embedding model
  @Query(
      "SELECT d.embeddingModel, COUNT(d) as count FROM DocumentMetric d WHERE d.embeddingModel IS NOT NULL GROUP BY d.embeddingModel ORDER BY count DESC")
  List<Object[]> getEmbeddingModelDistribution();
}
