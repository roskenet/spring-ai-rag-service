package com.zalando.rag.repository;

import com.zalando.rag.entity.SystemMetric;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

  // Get latest metric by type
  Optional<SystemMetric> findTopByMetricTypeOrderByRecordedAtDesc(String metricType);

  // Get metrics for a specific type in time range
  List<SystemMetric> findByMetricTypeAndRecordedAtBetweenOrderByRecordedAt(
      String metricType, LocalDateTime startTime, LocalDateTime endTime);

  // Average metric value for a type in time range
  @Query(
      "SELECT AVG(s.metricValue) FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime")
  Double getAverageMetricValue(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Max metric value for a type in time range
  @Query(
      "SELECT MAX(s.metricValue) FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime")
  Double getMaxMetricValue(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Min metric value for a type in time range
  @Query(
      "SELECT MIN(s.metricValue) FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime")
  Double getMinMetricValue(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Sum metric value for a type in time range (useful for counts)
  @Query(
      "SELECT SUM(s.metricValue) FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime")
  Double getSumMetricValue(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Hourly aggregated metrics
  @Query(
      "SELECT HOUR(s.recordedAt) as hour, AVG(s.metricValue) as avgValue FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime GROUP BY HOUR(s.recordedAt) ORDER BY hour")
  List<Object[]> getHourlyAverageMetrics(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Daily aggregated metrics
  @Query(
      "SELECT DATE(s.recordedAt) as date, AVG(s.metricValue) as avgValue FROM SystemMetric s WHERE s.metricType = :metricType AND s.recordedAt >= :startTime AND s.recordedAt <= :endTime GROUP BY DATE(s.recordedAt) ORDER BY date")
  List<Object[]> getDailyAverageMetrics(
      @Param("metricType") String metricType,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  // Clean up old metrics (for data retention)
  void deleteByRecordedAtBefore(LocalDateTime cutoffTime);

  // Get latest metrics for dashboard
  @Query(
      "SELECT s FROM SystemMetric s WHERE s.recordedAt = (SELECT MAX(s2.recordedAt) FROM SystemMetric s2 WHERE s2.metricType = s.metricType)")
  List<SystemMetric> getLatestMetricsForAllTypes();
}
