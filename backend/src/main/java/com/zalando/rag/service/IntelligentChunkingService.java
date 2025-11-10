package com.zalando.rag.service;

import com.zalando.rag.service.chunking.ChunkingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * Backward compatibility wrapper for the old IntelligentChunkingService. This service now delegates
 * to the new extensible ChunkingService using the "intelligent" strategy.
 *
 * @deprecated Use {@link ChunkingService} directly instead. This class will be removed in a future
 *     version.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Deprecated(since = "1.1.0", forRemoval = true)
public class IntelligentChunkingService {

  private final ChunkingService chunkingService;

  /**
   * Chunks a document using the intelligent strategy. This method is maintained for backward
   * compatibility.
   *
   * @param content the document content to chunk
   * @param filename the filename for metadata
   * @param title the document title for metadata
   * @return list of document chunks
   * @deprecated Use {@link ChunkingService#chunkDocument(String, String, String)} instead
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public List<Document> chunkDocument(String content, String filename, String title) {
    log.warn(
        "Using deprecated IntelligentChunkingService. Please migrate to ChunkingService for better performance and features.");

    // Delegate to the new chunking service with "intelligent" strategy
    return chunkingService.chunkDocument(content, filename, title, "intelligent");
  }
}
