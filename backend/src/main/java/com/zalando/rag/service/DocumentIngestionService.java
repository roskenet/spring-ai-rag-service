package com.zalando.rag.service;

import com.zalando.rag.config.RagProperties;
import com.zalando.rag.dto.ProcessingOptions;
import com.zalando.rag.entity.Document;
import com.zalando.rag.repository.DocumentRepository;
import com.zalando.rag.service.chunking.ChunkingProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

  private final DocumentRepository documentRepository;
  private final VectorStoreService vectorStoreService;
  private final RagProperties ragProperties;
  private final com.zalando.rag.service.chunking.ChunkingService chunkingService;
  private final ChunkingProperties chunkingProperties;

  @Transactional
  public Document ingestDocument(MultipartFile file) throws IOException {
    return ingestDocument(
        file, ProcessingOptions.defaultOptions(chunkingProperties.getDefaultStrategy()));
  }

  @Transactional
  public Document ingestDocument(MultipartFile file, ProcessingOptions processingOptions)
      throws IOException {
    log.info(
        "Starting ingestion of document: {} with processing options: {}",
        file.getOriginalFilename(),
        processingOptions.getChunkingStrategy());

    // Validate file
    validateFile(file);

    // Read file content
    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    String contentHash = calculateHash(content);

    // Check if document already exists
    Optional<Document> existingDoc = documentRepository.findByContentHash(contentHash);
    if (existingDoc.isPresent()) {
      Document existing = existingDoc.get();
      boolean needsReprocessing =
          existing.getStatus() == Document.DocumentStatus.FAILED
              || existing.getStatus() == Document.DocumentStatus.UPLOADED
              || (existing.getStatus() == Document.DocumentStatus.PROCESSED
                  && !vectorStoreService.hasChunks(existing.getId().toString()));
      if (!needsReprocessing) {
        log.info(
            "Document already exists and is indexed in vector store: {}", existing.getFilename());
        return existing;
      }
      log.info(
          "Document exists (status={}) but is missing from vector store, reprocessing: {}",
          existing.getStatus(),
          existing.getFilename());
      vectorStoreService.deleteByDocumentId(existing.getId().toString());
      processDocumentAsync(existing, processingOptions);
      return existing;
    }

    // Extract title from content (first non-empty line or filename)
    String title = extractTitle(content, file.getOriginalFilename());

    // Create document entity
    Document document =
        Document.builder()
            .filename(file.getOriginalFilename())
            .title(title)
            .content(content)
            .contentHash(contentHash)
            .fileSize(file.getSize())
            .status(Document.DocumentStatus.UPLOADED)
            .build();

    document = documentRepository.save(document);

    // Process document asynchronously with processing options
    processDocumentAsync(document, processingOptions);

    return document;
  }

  private void processDocumentAsync(Document document) {
    processDocumentAsync(document, ProcessingOptions.defaultOptions());
  }

  private void processDocumentAsync(Document document, ProcessingOptions processingOptions) {
    try {
      document.setStatus(Document.DocumentStatus.PROCESSING);
      documentRepository.save(document);

      // Use new chunking service with processing options
      List<org.springframework.ai.document.Document> chunks =
          chunkingService.chunkDocument(
              document.getContent(),
              document.getFilename(),
              document.getTitle(),
              processingOptions.getChunkingStrategy(),
              processingOptions.getChunkingConfig());

      // Validate chunk count if specified
      if (processingOptions.getMaxChunksPerDocument() > 0
          && chunks.size() > processingOptions.getMaxChunksPerDocument()) {
        throw new IllegalStateException(
            String.format(
                "Document generated %d chunks, exceeding limit of %d",
                chunks.size(), processingOptions.getMaxChunksPerDocument()));
      }

      // Add metadata to chunks
      for (int i = 0; i < chunks.size(); i++) {
        org.springframework.ai.document.Document chunk = chunks.get(i);

        // Basic document metadata
        chunk.getMetadata().put("document_id", document.getId().toString());
        chunk.getMetadata().put("filename", document.getFilename());
        chunk.getMetadata().put("title", document.getTitle());
        chunk.getMetadata().put("chunk_index", String.valueOf(i));
        chunk.getMetadata().put("total_chunks", String.valueOf(chunks.size()));

        // Add processing options metadata if requested
        if (processingOptions.isIncludeProcessingMetadata()) {
          chunk.getMetadata().put("processing_strategy", processingOptions.getChunkingStrategy());
          chunk.getMetadata().put("processing_timestamp", System.currentTimeMillis());
          if (processingOptions.getLanguageHint() != null) {
            chunk.getMetadata().put("language_hint", processingOptions.getLanguageHint());
          }
        }

        // Add custom metadata if provided
        if (processingOptions.getCustomMetadata() != null) {
          chunk.getMetadata().putAll(processingOptions.getCustomMetadata());
        }
      }

      // Store in vector database
      vectorStoreService.addDocuments(chunks);

      // Update document status
      document.setChunkCount(chunks.size());
      document.setStatus(Document.DocumentStatus.PROCESSED);
      documentRepository.save(document);

      log.info(
          "Successfully processed document: {} with {} chunks using strategy: {}",
          document.getFilename(),
          chunks.size(),
          processingOptions.getChunkingStrategy());

    } catch (Exception e) {
      log.error(
          "Error processing document: {} with strategy: {}",
          document.getFilename(),
          processingOptions.getChunkingStrategy(),
          e);
      document.setStatus(Document.DocumentStatus.FAILED);
      document.setErrorMessage(e.getMessage());
      documentRepository.save(document);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    String filename = file.getOriginalFilename();
    if (filename == null) {
      throw new IllegalArgumentException("Filename is null");
    }

    // Check file extension
    String extension = getFileExtension(filename).toLowerCase();
    if (!ragProperties.getAllowedFileTypes().contains(extension)) {
      throw new IllegalArgumentException(
          "File type not supported. Allowed types: " + ragProperties.getAllowedFileTypes());
    }

    // Check file size (simplified - would need proper parsing of maxFileSize)
    if (file.getSize() > 10 * 1024 * 1024) { // 10MB
      throw new IllegalArgumentException("File size exceeds maximum allowed size");
    }
  }

  private String extractTitle(String content, String filename) {
    // Try to extract title from markdown content
    String[] lines = content.split("\n");
    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("# ")) {
        return line.substring(2).trim();
      }
      if (!line.isEmpty() && !line.startsWith("#")) {
        // Use first non-empty, non-header line if no H1 found
        return line.length() > 50 ? line.substring(0, 50) + "..." : line;
      }
    }

    // Fallback to filename without extension
    return filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename;
  }

  private String getFileExtension(String filename) {
    int lastDotIndex = filename.lastIndexOf(".");
    return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
  }

  private String calculateHash(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  public List<Document> getAllDocuments() {
    return documentRepository.findAll();
  }

  public Optional<Document> getDocumentById(Long id) {
    return documentRepository.findById(id);
  }

  @Transactional
  public void deleteDocument(Long id) {
    Optional<Document> document = documentRepository.findById(id);
    if (document.isPresent()) {
      // Remove from vector store
      vectorStoreService.deleteByDocumentId(id.toString());
      // Remove from database
      documentRepository.deleteById(id);
      log.info("Deleted document: {}", document.get().getFilename());
    }
  }

  /** Gets available chunking strategies for client selection. */
  public List<com.zalando.rag.service.chunking.ChunkingStrategyRegistry.StrategyInfo>
      getAvailableChunkingStrategies() {
    return chunkingService.getAvailableStrategies();
  }

  /** Analyzes document content to recommend optimal processing options. */
  public ProcessingOptions recommendProcessingOptions(String content) {
    com.zalando.rag.service.chunking.DocumentAnalysis analysis =
        chunkingService.analyzeDocument(content);
    String recommendedStrategy = chunkingService.getRecommendedStrategy(analysis);

    // Build processing options based on document analysis
    ProcessingOptions.ProcessingOptionsBuilder builder =
        ProcessingOptions.builder().chunkingStrategy(recommendedStrategy);

    // Set config based on document type
    switch (analysis.getDocumentType()) {
      case TECHNICAL_GUIDE:
      case CODE_HEAVY:
      case API_DOCUMENTATION:
        builder.chunkingConfig(com.zalando.rag.service.chunking.ChunkingConfig.technicalConfig());
        break;
      case COMPREHENSIVE_DOC:
        builder.chunkingConfig(com.zalando.rag.service.chunking.ChunkingConfig.largeChunkConfig());
        break;
      case SIMPLE_TEXT:
        builder.chunkingConfig(com.zalando.rag.service.chunking.ChunkingConfig.simpleConfig());
        break;
      default:
        builder.chunkingConfig(com.zalando.rag.service.chunking.ChunkingConfig.defaultConfig());
    }

    return builder.build();
  }

  /** Analyzes document content without processing it. */
  public com.zalando.rag.service.chunking.DocumentAnalysis analyzeDocument(String content) {
    return chunkingService.analyzeDocument(content);
  }
}
