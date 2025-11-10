package com.zalando.rag.service;

import com.zalando.rag.entity.DocumentMetric;
import com.zalando.rag.entity.QueryMetric;
import com.zalando.rag.entity.SystemMetric;
import com.zalando.rag.repository.DocumentMetricRepository;
import com.zalando.rag.repository.QueryMetricRepository;
import com.zalando.rag.repository.SystemMetricRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

  private final QueryMetricRepository queryMetricRepository;
  private final DocumentMetricRepository documentMetricRepository;
  private final SystemMetricRepository systemMetricRepository;

  // Query Analytics Methods
  @Transactional
  public void recordQueryMetric(QueryMetric queryMetric) {
    try {
      queryMetricRepository.save(queryMetric);
      log.debug(
          "Recorded query metric: query='{}', responseTime={}ms, success={}",
          queryMetric.getQueryText(),
          queryMetric.getResponseTimeMs(),
          queryMetric.getSuccess());
    } catch (Exception e) {
      log.error("Failed to record query metric", e);
    }
  }

  public Map<String, Object> getQueryAnalytics(LocalDateTime startTime, LocalDateTime endTime) {
    Map<String, Object> analytics = new HashMap<>();

    try {
      // Basic metrics
      long totalQueries = queryMetricRepository.countQueriesInTimeRange(startTime, endTime);
      Double avgResponseTime = queryMetricRepository.getAverageResponseTime(startTime, endTime);
      Double avgAccuracy = queryMetricRepository.getAverageAccuracy(startTime, endTime);
      Double successRate = queryMetricRepository.getSuccessRate(startTime, endTime);

      analytics.put("totalQueries", totalQueries);
      analytics.put("averageResponseTime", avgResponseTime != null ? avgResponseTime : 0.0);
      analytics.put("averageAccuracy", avgAccuracy != null ? avgAccuracy : 0.0);
      analytics.put("successRate", successRate != null ? successRate : 0.0);

      // Hourly distribution
      List<Object[]> hourlyDistribution =
          queryMetricRepository.getHourlyQueryDistribution(startTime, endTime);
      analytics.put("hourlyDistribution", hourlyDistribution);

      // Most frequent queries
      List<Object[]> frequentQueries =
          queryMetricRepository.getMostFrequentQueries(startTime, endTime, PageRequest.of(0, 10));
      analytics.put("frequentQueries", frequentQueries);

      // Recent failures for debugging
      List<QueryMetric> recentFailures =
          queryMetricRepository.findBySuccessFalseOrderByCreatedAtDesc(PageRequest.of(0, 5));
      analytics.put("recentFailures", recentFailures);

    } catch (Exception e) {
      log.error("Error generating query analytics", e);
      analytics.put("error", "Failed to generate query analytics");
    }

    return analytics;
  }

  /** Get hourly performance metrics for time-series charts (accuracy + response time) */
  public List<Map<String, Object>> getHourlyPerformanceMetrics(
      LocalDateTime startTime, LocalDateTime endTime) {
    List<Map<String, Object>> hourlyMetrics = new ArrayList<>();

    try {
      // Generate hourly time slots between start and end time
      LocalDateTime current = startTime.withMinute(0).withSecond(0).withNano(0);
      LocalDateTime end = endTime.withMinute(59).withSecond(59);

      while (current.isBefore(end)) {
        LocalDateTime hourEnd = current.plusHours(1);

        // Get metrics for this hour
        Double avgResponseTime = queryMetricRepository.getAverageResponseTime(current, hourEnd);
        Double avgAccuracy = queryMetricRepository.getAverageAccuracy(current, hourEnd);
        Long queryCount = queryMetricRepository.countQueriesInTimeRange(current, hourEnd);

        Map<String, Object> hourData = new HashMap<>();
        hourData.put("time", current.getHour() + ":00");
        hourData.put("hour", current.getHour());
        hourData.put("avgResponse", avgResponseTime != null ? avgResponseTime.intValue() : 0);
        hourData.put("accuracy", avgAccuracy != null ? avgAccuracy : 0.0);
        hourData.put("queries", queryCount != null ? queryCount.intValue() : 0);

        hourlyMetrics.add(hourData);
        current = current.plusHours(1);
      }

      log.debug(
          "Generated {} hourly performance metrics from {} to {}",
          hourlyMetrics.size(),
          startTime,
          endTime);

    } catch (Exception e) {
      log.error("Error generating hourly performance metrics", e);
    }

    return hourlyMetrics;
  }

  // Document Analytics Methods
  @Transactional
  public void recordDocumentMetric(DocumentMetric documentMetric) {
    try {
      // Check if metric already exists for this document
      Optional<DocumentMetric> existing =
          documentMetricRepository.findByDocumentId(documentMetric.getDocumentId());
      if (existing.isPresent()) {
        // Update existing metric
        DocumentMetric existingMetric = existing.get();
        existingMetric.setFileSize(documentMetric.getFileSize());
        existingMetric.setChunkCount(documentMetric.getChunkCount());
        existingMetric.setProcessingTimeMs(documentMetric.getProcessingTimeMs());
        existingMetric.setChunkingStrategy(documentMetric.getChunkingStrategy());
        existingMetric.setEmbeddingModel(documentMetric.getEmbeddingModel());
        existingMetric.setCategory(documentMetric.getCategory());
        documentMetricRepository.save(existingMetric);
      } else {
        documentMetricRepository.save(documentMetric);
      }
      log.debug(
          "Recorded document metric: document='{}', chunks={}, size={}",
          documentMetric.getDocumentName(),
          documentMetric.getChunkCount(),
          documentMetric.getFileSize());
    } catch (Exception e) {
      log.error("Failed to record document metric", e);
    }
  }

  @Transactional
  public void recordDocumentAccess(Long documentId) {
    try {
      documentMetricRepository.incrementAccessCount(documentId, LocalDateTime.now());
      log.debug("Recorded document access for documentId: {}", documentId);
    } catch (Exception e) {
      log.error("Failed to record document access for documentId: {}", documentId, e);
    }
  }

  public Map<String, Object> getDocumentAnalytics(LocalDateTime startTime, LocalDateTime endTime) {
    Map<String, Object> analytics = new HashMap<>();

    try {
      // Growth metrics
      List<Object[]> documentGrowth =
          documentMetricRepository.getDocumentGrowthByDay(startTime, endTime);
      List<Object[]> chunkGrowth = documentMetricRepository.getChunkGrowthByDay(startTime, endTime);
      analytics.put("documentGrowth", documentGrowth);
      analytics.put("chunkGrowth", chunkGrowth);

      // Distribution metrics
      List<Object[]> typeDistribution = documentMetricRepository.getDocumentTypeDistribution();
      List<Object[]> fileTypeDistribution = documentMetricRepository.getFileTypeDistribution();
      analytics.put("categoryDistribution", typeDistribution);
      analytics.put("fileTypeDistribution", fileTypeDistribution);

      // Access patterns
      List<Object[]> mostAccessed =
          documentMetricRepository.getMostAccessedDocuments(PageRequest.of(0, 10));
      analytics.put("mostAccessedDocuments", mostAccessed);

      // Storage and processing
      Long totalStorage = documentMetricRepository.getTotalStorageUsed();
      Long totalChunks = documentMetricRepository.getTotalChunkCount();
      Long totalDocuments = documentMetricRepository.getTotalDocumentCount();
      Double avgProcessingTime = documentMetricRepository.getAverageProcessingTime();

      analytics.put("totalStorageUsed", totalStorage != null ? totalStorage : 0L);
      analytics.put("totalChunks", totalChunks != null ? totalChunks : 0L);
      analytics.put("totalDocuments", totalDocuments != null ? totalDocuments : 0L);
      analytics.put("averageProcessingTime", avgProcessingTime != null ? avgProcessingTime : 0.0);

      // Strategy usage
      List<Object[]> chunkingStrategies =
          documentMetricRepository.getChunkingStrategyDistribution();
      List<Object[]> embeddingModels = documentMetricRepository.getEmbeddingModelDistribution();
      analytics.put("chunkingStrategyUsage", chunkingStrategies);
      analytics.put("embeddingModelUsage", embeddingModels);

    } catch (Exception e) {
      log.error("Error generating document analytics", e);
      analytics.put("error", "Failed to generate document analytics");
    }

    return analytics;
  }

  // System Analytics Methods
  @Transactional
  public void recordSystemMetric(
      SystemMetric.MetricType type, double value, String unit, String metadata) {
    try {
      SystemMetric metric =
          SystemMetric.builder()
              .metricType(type.name())
              .metricValue(value)
              .metricUnit(unit)
              .metadata(metadata)
              .build();
      systemMetricRepository.save(metric);
      log.debug("Recorded system metric: type={}, value={}", type, value);
    } catch (Exception e) {
      log.error("Failed to record system metric: type={}, value={}", type, value, e);
    }
  }

  public Map<String, Object> getSystemAnalytics(LocalDateTime startTime, LocalDateTime endTime) {
    Map<String, Object> analytics = new HashMap<>();

    try {
      // Current system health
      Map<String, Object> currentMetrics = new HashMap<>();
      for (SystemMetric.MetricType type : SystemMetric.MetricType.values()) {
        Optional<SystemMetric> latest =
            systemMetricRepository.findTopByMetricTypeOrderByRecordedAtDesc(type.name());
        if (latest.isPresent()) {
          currentMetrics.put(type.name().toLowerCase(), latest.get().getMetricValue());
        }
      }
      analytics.put("currentMetrics", currentMetrics);

      // Performance trends
      Map<String, Object> trends = new HashMap<>();
      trends.put(
          "responseTime",
          systemMetricRepository.getHourlyAverageMetrics(
              SystemMetric.MetricType.AVERAGE_RESPONSE_TIME_MS.name(), startTime, endTime));
      trends.put(
          "queriesPerMinute",
          systemMetricRepository.getHourlyAverageMetrics(
              SystemMetric.MetricType.QUERIES_PER_MINUTE.name(), startTime, endTime));
      trends.put(
          "memoryUsage",
          systemMetricRepository.getHourlyAverageMetrics(
              SystemMetric.MetricType.MEMORY_USAGE_MB.name(), startTime, endTime));
      trends.put(
          "cpuUsage",
          systemMetricRepository.getHourlyAverageMetrics(
              SystemMetric.MetricType.CPU_USAGE_PERCENT.name(), startTime, endTime));
      analytics.put("trends", trends);

      // Basic system metrics
      Map<String, Object> basicMetrics = new HashMap<>();
      basicMetrics.put(
          "avgTotalLatency",
          systemMetricRepository.getAverageMetricValue(
              SystemMetric.MetricType.TOTAL_LATENCY_MS.name(), startTime, endTime));
      basicMetrics.put(
          "avgRetrievalLatency",
          systemMetricRepository.getAverageMetricValue(
              SystemMetric.MetricType.RETRIEVAL_LATENCY_MS.name(), startTime, endTime));

      // Error rate calculation
      Double errorCount =
          systemMetricRepository.getSumMetricValue(
              SystemMetric.MetricType.ERROR_COUNT.name(), startTime, endTime);
      Double successCount =
          systemMetricRepository.getSumMetricValue(
              SystemMetric.MetricType.SUCCESS_COUNT.name(), startTime, endTime);
      double totalRequests =
          (errorCount != null ? errorCount : 0) + (successCount != null ? successCount : 0);
      double errorRate =
          totalRequests > 0 ? ((errorCount != null ? errorCount : 0) / totalRequests) * 100 : 0;
      basicMetrics.put("errorRate", errorRate);

      basicMetrics.put(
          "totalTokens",
          systemMetricRepository.getSumMetricValue(
              SystemMetric.MetricType.TOTAL_TOKENS.name(), startTime, endTime));
      basicMetrics.put(
          "avgDocumentsRetrieved",
          systemMetricRepository.getAverageMetricValue(
              SystemMetric.MetricType.DOCUMENTS_RETRIEVED.name(), startTime, endTime));
      analytics.put("basicMetrics", basicMetrics);

      // Aggregated statistics (legacy)
      Map<String, Object> aggregates = new HashMap<>();
      aggregates.put(
          "avgResponseTime",
          systemMetricRepository.getAverageMetricValue(
              SystemMetric.MetricType.AVERAGE_RESPONSE_TIME_MS.name(), startTime, endTime));
      aggregates.put(
          "maxMemoryUsage",
          systemMetricRepository.getMaxMetricValue(
              SystemMetric.MetricType.MEMORY_USAGE_MB.name(), startTime, endTime));
      aggregates.put(
          "avgCpuUsage",
          systemMetricRepository.getAverageMetricValue(
              SystemMetric.MetricType.CPU_USAGE_PERCENT.name(), startTime, endTime));
      analytics.put("aggregates", aggregates);

    } catch (Exception e) {
      log.error("Error generating system analytics", e);
      analytics.put("error", "Failed to generate system analytics");
    }

    return analytics;
  }

  public Map<String, Object> getDashboardSummary() {
    LocalDateTime endTime = LocalDateTime.now();
    LocalDateTime startTime24h = endTime.minusHours(24);
    LocalDateTime startTime7d = endTime.minusDays(7);

    Map<String, Object> summary = new HashMap<>();

    try {
      // 24-hour metrics
      Map<String, Object> last24h = new HashMap<>();
      last24h.put("queries", queryMetricRepository.countQueriesInTimeRange(startTime24h, endTime));
      last24h.put(
          "avgResponseTime", queryMetricRepository.getAverageResponseTime(startTime24h, endTime));
      last24h.put("avgAccuracy", queryMetricRepository.getAverageAccuracy(startTime24h, endTime));
      summary.put("last24Hours", last24h);

      // 7-day metrics
      Map<String, Object> last7d = new HashMap<>();
      last7d.put("queries", queryMetricRepository.countQueriesInTimeRange(startTime7d, endTime));
      last7d.put(
          "avgResponseTime", queryMetricRepository.getAverageResponseTime(startTime7d, endTime));
      last7d.put("avgAccuracy", queryMetricRepository.getAverageAccuracy(startTime7d, endTime));
      summary.put("last7Days", last7d);

      // Overall system stats
      summary.put("totalDocuments", documentMetricRepository.getTotalDocumentCount());
      summary.put("totalChunks", documentMetricRepository.getTotalChunkCount());
      summary.put("totalStorage", documentMetricRepository.getTotalStorageUsed());

    } catch (Exception e) {
      log.error("Error generating dashboard summary", e);
      summary.put("error", "Failed to generate dashboard summary");
    }

    return summary;
  }
}
