package com.zalando.rag.repository;

import com.zalando.rag.entity.QueryMetric;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryMetricRepository extends JpaRepository<QueryMetric, Long> {

  // Query volume analytics
  @Query(
      "SELECT COUNT(q) FROM QueryMetric q WHERE q.createdAt >= :startTime AND q.createdAt <= :endTime")
  long countQueriesInTimeRange(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  // Performance analytics
  @Query(
      "SELECT AVG(q.responseTimeMs) FROM QueryMetric q WHERE q.success = true AND q.createdAt >= :startTime AND q.createdAt <= :endTime")
  Double getAverageResponseTime(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  @Query(
      "SELECT AVG(q.accuracyScore) FROM QueryMetric q WHERE q.accuracyScore IS NOT NULL AND q.createdAt >= :startTime AND q.createdAt <= :endTime")
  Double getAverageAccuracy(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  // Success rate
  @Query(
      "SELECT (COUNT(q) * 100.0) / (SELECT COUNT(q2) FROM QueryMetric q2 WHERE q2.createdAt >= :startTime AND q2.createdAt <= :endTime) FROM QueryMetric q WHERE q.success = true AND q.createdAt >= :startTime AND q.createdAt <= :endTime")
  Double getSuccessRate(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  // Hourly query distribution
  @Query(
      "SELECT HOUR(q.createdAt) as hour, COUNT(q) as count FROM QueryMetric q WHERE q.createdAt >= :startTime AND q.createdAt <= :endTime GROUP BY HOUR(q.createdAt) ORDER BY hour")
  List<Object[]> getHourlyQueryDistribution(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  // Most common query patterns (basic text analysis)
  @Query(
      "SELECT q.queryText, COUNT(q) as frequency FROM QueryMetric q WHERE q.createdAt >= :startTime AND q.createdAt <= :endTime GROUP BY q.queryText ORDER BY frequency DESC")
  List<Object[]> getMostFrequentQueries(
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime,
      org.springframework.data.domain.Pageable pageable);

  // Recent failed queries for debugging
  List<QueryMetric> findBySuccessFalseOrderByCreatedAtDesc(
      org.springframework.data.domain.Pageable pageable);

  // User session analytics
  @Query(
      "SELECT q.userSessionId, COUNT(q) as queryCount FROM QueryMetric q WHERE q.userSessionId IS NOT NULL AND q.createdAt >= :startTime AND q.createdAt <= :endTime GROUP BY q.userSessionId ORDER BY queryCount DESC")
  List<Object[]> getSessionQueryCounts(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
