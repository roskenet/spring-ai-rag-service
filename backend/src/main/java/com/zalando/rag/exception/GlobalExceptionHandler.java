package com.zalando.rag.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    Map<String, Object> response = new HashMap<>();
    response.put("error", "Validation failed");
    response.put("message", ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    response.put("status", HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex) {

    Map<String, Object> response = new HashMap<>();
    response.put("error", "File too large");
    response.put("message", "The uploaded file exceeds the maximum allowed size");
    response.put("status", HttpStatus.BAD_REQUEST.value());

    log.warn("File upload size exceeded: {}", ex.getMessage());

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException ex) {

    Map<String, Object> response = new HashMap<>();
    response.put("error", "Invalid argument");
    response.put("message", ex.getMessage());
    response.put("status", HttpStatus.BAD_REQUEST.value());

    log.warn("Invalid argument: {}", ex.getMessage());

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {

    Map<String, Object> response = new HashMap<>();
    response.put("error", "Internal server error");
    response.put("message", "An unexpected error occurred");
    response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

    log.error("Unexpected error occurred", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
