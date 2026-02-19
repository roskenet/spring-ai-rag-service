package com.zalando.rag.controller;

import com.zalando.rag.dto.DocumentDto;
import com.zalando.rag.dto.DocumentUploadResponse;
import com.zalando.rag.entity.Document;
import com.zalando.rag.service.DocumentIngestionService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend to access document endpoints
public class DocumentController {

  private final DocumentIngestionService documentIngestionService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DocumentUploadResponse> uploadDocument(
      @RequestParam("file") MultipartFile file) {

    try {
      log.info("Received file upload request: {}", file.getOriginalFilename());

      Document document = documentIngestionService.ingestDocument(file);

      DocumentUploadResponse response =
          DocumentUploadResponse.builder()
              .documentId(document.getId())
              .filename(document.getFilename())
              .message("Document uploaded successfully and processing started")
              .success(true)
              .status(document.getStatus().name())
              .build();

      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid file upload request: {}", e.getMessage());

      DocumentUploadResponse response =
          DocumentUploadResponse.builder()
              .filename(file.getOriginalFilename())
              .message(e.getMessage())
              .success(false)
              .build();

      return ResponseEntity.badRequest().body(response);

    } catch (Exception e) {
      log.error("Error uploading document: {}", file.getOriginalFilename(), e);

      DocumentUploadResponse response =
          DocumentUploadResponse.builder()
              .filename(file.getOriginalFilename())
              .message("Internal server error occurred while uploading document")
              .success(false)
              .build();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @GetMapping
  public ResponseEntity<List<DocumentDto>> getAllDocuments() {
    try {
      List<Document> documents = documentIngestionService.getAllDocuments();
      List<DocumentDto> documentDtos =
          documents.stream().map(DocumentDto::fromEntity).collect(Collectors.toList());

      return ResponseEntity.ok(documentDtos);

    } catch (Exception e) {
      log.error("Error retrieving documents", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<DocumentDto> getDocumentById(@PathVariable Long id) {
    try {
      Optional<Document> document = documentIngestionService.getDocumentById(id);

      if (document.isPresent()) {
        DocumentDto documentDto = DocumentDto.fromEntity(document.get());
        return ResponseEntity.ok(documentDto);
      } else {
        return ResponseEntity.notFound().build();
      }

    } catch (Exception e) {
      log.error("Error retrieving document with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
    try {
      Optional<Document> document = documentIngestionService.getDocumentById(id);

      if (document.isPresent()) {
        documentIngestionService.deleteDocument(id);
        log.info("Deleted document with id: {}", id);
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }

    } catch (Exception e) {
      log.error("Error deleting document with id: {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/status/{status}")
  public ResponseEntity<List<DocumentDto>> getDocumentsByStatus(@PathVariable String status) {

    try {
      Document.DocumentStatus documentStatus =
          Document.DocumentStatus.valueOf(status.toUpperCase());
      // Note: This would require adding a method to the service
      // For now, we'll return all documents and filter client-side
      List<Document> allDocuments = documentIngestionService.getAllDocuments();
      List<DocumentDto> filteredDocuments =
          allDocuments.stream()
              .filter(doc -> doc.getStatus() == documentStatus)
              .map(DocumentDto::fromEntity)
              .collect(Collectors.toList());

      return ResponseEntity.ok(filteredDocuments);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid document status: {}", status);
      return ResponseEntity.badRequest().build();

    } catch (Exception e) {
      log.error("Error retrieving documents with status: {}", status, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
