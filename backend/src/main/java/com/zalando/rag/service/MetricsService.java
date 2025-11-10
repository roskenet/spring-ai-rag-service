package com.zalando.rag.service;

import com.zalando.rag.entity.SystemMetric;
import com.zalando.rag.repository.SystemMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

  private final SystemMetricRepository systemMetricRepository;

  /** Record total query latency (end-to-end response time) */
  public void recordTotalLatency(long latencyMs) {
    recordMetric(SystemMetric.MetricType.TOTAL_LATENCY_MS.name(), latencyMs);
  }

  /** Record retrieval latency (time to search and retrieve documents) */
  public void recordRetrievalLatency(long latencyMs) {
    recordMetric(SystemMetric.MetricType.RETRIEVAL_LATENCY_MS.name(), latencyMs);
  }

  /** Record query error (increment error count) */
  public void recordError(String errorType) {
    recordMetric(SystemMetric.MetricType.ERROR_COUNT.name(), 1, "error_type=" + errorType);
  }

  /** Record successful query (increment success count) */
  public void recordSuccess() {
    recordMetric(SystemMetric.MetricType.SUCCESS_COUNT.name(), 1);
  }

  /** Record token usage for cost tracking */
  public void recordTokenUsage(int inputTokens, int outputTokens) {
    recordMetric(SystemMetric.MetricType.INPUT_TOKENS.name(), inputTokens);
    recordMetric(SystemMetric.MetricType.OUTPUT_TOKENS.name(), outputTokens);
    recordMetric(SystemMetric.MetricType.TOTAL_TOKENS.name(), inputTokens + outputTokens);
  }

  /** Record cost per query (if available) */
  public void recordQueryCost(double costUsd) {
    recordMetric(SystemMetric.MetricType.QUERY_COST_USD.name(), costUsd);
  }

  /** Record number of documents retrieved */
  public void recordDocumentsRetrieved(int count) {
    recordMetric(SystemMetric.MetricType.DOCUMENTS_RETRIEVED.name(), count);
  }

  /** Private helper method to record metrics */
  private void recordMetric(String metricType, double value) {
    recordMetric(metricType, value, null);
  }

  private void recordMetric(String metricType, double value, String metadata) {
    try {
      SystemMetric metric =
          SystemMetric.builder()
              .metricType(metricType)
              .metricValue(value)
              .metricUnit(getUnitForMetricType(metricType))
              .metadata(metadata)
              .build();

      systemMetricRepository.save(metric);
      log.debug("Recorded metric: {} = {} {}", metricType, value, getUnitForMetricType(metricType));
    } catch (Exception e) {
      log.error("Failed to record metric: {} = {}", metricType, value, e);
      // Don't fail the request if metrics recording fails
    }
  }

  /** Get the appropriate unit for each metric type */
  private String getUnitForMetricType(String metricType) {
    return switch (metricType) {
      case "TOTAL_LATENCY_MS", "RETRIEVAL_LATENCY_MS" -> "ms";
      case "ERROR_COUNT", "SUCCESS_COUNT", "DOCUMENTS_RETRIEVED" -> "count";
      case "INPUT_TOKENS", "OUTPUT_TOKENS", "TOTAL_TOKENS" -> "tokens";
      case "QUERY_COST_USD" -> "usd";
      default -> "value";
    };
  }
}
