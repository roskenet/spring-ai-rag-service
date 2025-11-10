package com.zalando.rag.controller;

import com.zalando.rag.service.AnalyticsService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to access analytics endpoints
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/dashboard")
  public ResponseEntity<Map<String, Object>> getDashboardSummary() {
    try {
      Map<String, Object> summary = analyticsService.getDashboardSummary();
      return ResponseEntity.ok(summary);
    } catch (Exception e) {
      log.error("Error retrieving dashboard summary", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/queries")
  public ResponseEntity<Map<String, Object>> getQueryAnalytics(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startTime,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endTime) {

    try {
      // Default to last 7 days if no time range specified
      if (startTime == null) {
        startTime = LocalDateTime.now().minusDays(7);
      }
      if (endTime == null) {
        endTime = LocalDateTime.now();
      }

      Map<String, Object> analytics = analyticsService.getQueryAnalytics(startTime, endTime);
      return ResponseEntity.ok(analytics);
    } catch (Exception e) {
      log.error("Error retrieving query analytics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/documents")
  public ResponseEntity<Map<String, Object>> getDocumentAnalytics(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startTime,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endTime) {

    try {
      // Default to last 30 days if no time range specified
      if (startTime == null) {
        startTime = LocalDateTime.now().minusDays(30);
      }
      if (endTime == null) {
        endTime = LocalDateTime.now();
      }

      Map<String, Object> analytics = analyticsService.getDocumentAnalytics(startTime, endTime);
      return ResponseEntity.ok(analytics);
    } catch (Exception e) {
      log.error("Error retrieving document analytics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/system")
  public ResponseEntity<Map<String, Object>> getSystemAnalytics(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startTime,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endTime) {

    try {
      // Default to last 24 hours if no time range specified
      if (startTime == null) {
        startTime = LocalDateTime.now().minusHours(24);
      }
      if (endTime == null) {
        endTime = LocalDateTime.now();
      }

      Map<String, Object> analytics = analyticsService.getSystemAnalytics(startTime, endTime);
      return ResponseEntity.ok(analytics);
    } catch (Exception e) {
      log.error("Error retrieving system analytics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/performance")
  public ResponseEntity<Map<String, Object>> getPerformanceMetrics(
      @RequestParam(defaultValue = "24") int hours) {

    try {
      LocalDateTime endTime = LocalDateTime.now();
      LocalDateTime startTime = endTime.minusHours(hours);

      Map<String, Object> queryAnalytics = analyticsService.getQueryAnalytics(startTime, endTime);
      Map<String, Object> systemAnalytics = analyticsService.getSystemAnalytics(startTime, endTime);

      // Combine performance-related metrics
      Map<String, Object> performance =
          Map.of(
              "timeRange",
                  Map.of(
                      "startTime", startTime,
                      "endTime", endTime,
                      "hours", hours),
              "queries",
                  Map.of(
                      "totalQueries", queryAnalytics.get("totalQueries"),
                      "averageResponseTime", queryAnalytics.get("averageResponseTime"),
                      "averageAccuracy", queryAnalytics.get("averageAccuracy"),
                      "successRate", queryAnalytics.get("successRate"),
                      "hourlyDistribution", queryAnalytics.get("hourlyDistribution")),
              "system", systemAnalytics.get("trends"));

      return ResponseEntity.ok(performance);
    } catch (Exception e) {
      log.error("Error retrieving performance metrics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/usage")
  public ResponseEntity<Map<String, Object>> getUsageAnalytics(
      @RequestParam(defaultValue = "7") int days) {

    try {
      LocalDateTime endTime = LocalDateTime.now();
      LocalDateTime startTime = endTime.minusDays(days);

      Map<String, Object> queryAnalytics = analyticsService.getQueryAnalytics(startTime, endTime);
      Map<String, Object> documentAnalytics =
          analyticsService.getDocumentAnalytics(startTime, endTime);

      // Combine usage-related metrics
      Map<String, Object> usage =
          Map.of(
              "timeRange",
                  Map.of(
                      "startTime", startTime,
                      "endTime", endTime,
                      "days", days),
              "queries",
                  Map.of(
                      "totalQueries", queryAnalytics.get("totalQueries"),
                      "frequentQueries", queryAnalytics.get("frequentQueries"),
                      "hourlyDistribution", queryAnalytics.get("hourlyDistribution")),
              "documents",
                  Map.of(
                      "totalDocuments", documentAnalytics.get("totalDocuments"),
                      "totalChunks", documentAnalytics.get("totalChunks"),
                      "mostAccessedDocuments", documentAnalytics.get("mostAccessedDocuments"),
                      "categoryDistribution", documentAnalytics.get("categoryDistribution"),
                      "documentGrowth", documentAnalytics.get("documentGrowth")));

      return ResponseEntity.ok(usage);
    } catch (Exception e) {
      log.error("Error retrieving usage analytics", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
