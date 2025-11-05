package com.zalando.rag.controller;

import com.zalando.rag.dto.ChatRequest;
import com.zalando.rag.dto.ChatResponse;
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
public class ChatController {

  private final RagService ragService;

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

      return ResponseEntity.badRequest().body(errorResponse);
    }

    try {
      log.info("Received chat request: {}", request.getQuestion());

      ChatResponse response = ragService.askQuestion(request);

      log.info("Successfully processed chat request in {}ms", response.getResponseTimeMs());
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
}
