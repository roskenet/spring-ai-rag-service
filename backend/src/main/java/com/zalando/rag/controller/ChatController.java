package com.zalando.rag.controller;

import com.zalando.rag.dto.ChatRequest;
import com.zalando.rag.dto.ChatResponse;
import com.zalando.rag.entity.QueryMetric;
import com.zalando.rag.service.AnalyticsService;
import com.zalando.rag.service.ConfigurationService;
import com.zalando.rag.service.MetricsService;
import com.zalando.rag.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to access chat endpoints
public class ChatController {

  private final RagService ragService;
  private final AnalyticsService analyticsService;
  private final ConfigurationService configurationService;
  private final MetricsService metricsService;

  @PostMapping("/ask")
  public ResponseEntity<ChatResponse> askQuestion(
      @Valid @RequestBody ChatRequest request, BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      log.warn("Invalid chat request: {}", bindingResult.getAllErrors());

      ChatResponse errorResponse =
          ChatResponse.builder()
              .question(request != null ? request.getQuestion() : "")
              .answer("Invalid request: " + bindingResult.getAllErrors().get(0).getDefaultMessage())
              .responseTimeMs(0L)
              .build();

      // Record failed query metric
      recordQueryMetric(request, errorResponse, false, "Validation error");

      // Record error metrics
      metricsService.recordError("validation_error");

      return ResponseEntity.badRequest().body(errorResponse);
    }

    try {
      log.info("Received chat request: {}", request.getQuestion());

      // Apply configuration defaults if not provided
      enrichRequestWithDefaults(request);

      ChatResponse response = ragService.askQuestion(request);

      log.info("Successfully processed chat request in {}ms", response.getResponseTimeMs());

      // Record successful query metric
      recordQueryMetric(request, response, true, null);

      // Record success metrics
      metricsService.recordSuccess();
      metricsService.recordTotalLatency(response.getResponseTimeMs());
      metricsService.recordDocumentsRetrieved(
          response.getSources() != null ? response.getSources().size() : 0);

      // TODO: Track retrieval latency separately and token usage when available
      // metricsService.recordRetrievalLatency(retrievalTimeMs);
      // metricsService.recordTokenUsage(inputTokens, outputTokens);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error processing chat request: {}", request.getQuestion(), e);

      ChatResponse errorResponse =
          ChatResponse.builder()
              .question(request.getQuestion())
              .answer(
                  "I'm sorry, but I encountered an error while processing your question. Please try again later.")
              .responseTimeMs(0L)
              .build();

      // Record failed query metric
      recordQueryMetric(request, errorResponse, false, e.getMessage());

      // Record error metrics
      metricsService.recordError("internal_server_error");

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  @PostMapping("/ask/{documentId}")
  public ResponseEntity<ChatResponse> askQuestionAboutDocument(
      @PathVariable String documentId,
      @Valid @RequestBody ChatRequest request,
      BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      log.warn(
          "Invalid chat request for document {}: {}", documentId, bindingResult.getAllErrors());

      ChatResponse errorResponse =
          ChatResponse.builder()
              .question(request != null ? request.getQuestion() : "")
              .answer("Invalid request: " + bindingResult.getAllErrors().get(0).getDefaultMessage())
              .responseTimeMs(0L)
              .build();

      return ResponseEntity.badRequest().body(errorResponse);
    }

    try {
      log.info("Received chat request for document {}: {}", documentId, request.getQuestion());

      ChatResponse response = ragService.askQuestionWithinDocument(documentId, request);

      log.info(
          "Successfully processed chat request for document {} in {}ms",
          documentId,
          response.getResponseTimeMs());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error(
          "Error processing chat request for document {}: {}",
          documentId,
          request.getQuestion(),
          e);

      ChatResponse errorResponse =
          ChatResponse.builder()
              .question(request.getQuestion())
              .answer(
                  "I'm sorry, but I encountered an error while processing your question about this document. Please try again later.")
              .responseTimeMs(0L)
              .build();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Chat service is healthy");
  }

  private void enrichRequestWithDefaults(ChatRequest request) {
    // Apply configuration defaults if not provided in request
    if (request.getSelectedModel() == null) {
      request.setSelectedModel(configurationService.getActiveSelectedModel());
    }
    if (request.getTemperature() == 0.0) {
      request.setTemperature(configurationService.getActiveTemperature());
    }
    if (request.getSimilarityThreshold() == 0.0) {
      request.setSimilarityThreshold(configurationService.getActiveSimilarityThreshold());
    }
    if (request.getMaxResults() == 0) {
      request.setMaxResults(configurationService.getActiveMaxResults());
    }
  }

  private void recordQueryMetric(
      ChatRequest request, ChatResponse response, boolean success, String errorMessage) {
    try {
      QueryMetric metric =
          QueryMetric.builder()
              .queryText(request.getQuestion())
              .responseTimeMs(response.getResponseTimeMs())
              .similarityThreshold(request.getSimilarityThreshold())
              .maxResults(request.getMaxResults())
              .selectedModel(request.getSelectedModel())
              .temperature(request.getTemperature())
              .success(success)
              .errorMessage(errorMessage)
              .userSessionId(request.getSessionId())
              .build();

      // Calculate results found and accuracy score from response if available
      if (response.getSources() != null && !response.getSources().isEmpty()) {
        metric.setResultsFound(response.getSources().size());

        // Calculate accuracy based on average similarity score of source documents
        double totalSimilarity =
            response.getSources().stream().mapToDouble(source -> source.getSimilarity()).sum();
        double averageSimilarity = totalSimilarity / response.getSources().size();

        // Convert similarity score (0-1) to accuracy percentage and store as decimal (0-1)
        // Apply a scaling function to make the accuracy more meaningful:
        // - Similarity above 0.8 → High accuracy (0.8-1.0)
        // - Similarity 0.6-0.8 → Medium accuracy (0.6-0.8)
        // - Similarity below 0.6 → Lower accuracy (0.0-0.6)
        double accuracyScore = Math.max(0.0, Math.min(1.0, averageSimilarity));

        metric.setAccuracyScore(accuracyScore);

        log.debug(
            "Calculated accuracy score: {} from average similarity: {} across {} sources",
            accuracyScore,
            averageSimilarity,
            response.getSources().size());
      } else {
        metric.setResultsFound(0);
        // No sources means no relevant content found, set low accuracy
        metric.setAccuracyScore(0.0);
      }

      analyticsService.recordQueryMetric(metric);

    } catch (Exception e) {
      log.error("Failed to record query analytics", e);
      // Don't fail the request if analytics recording fails
    }
  }
}
