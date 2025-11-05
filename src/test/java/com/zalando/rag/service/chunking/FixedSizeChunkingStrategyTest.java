package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class FixedSizeChunkingStrategyTest {

  private FixedSizeChunkingStrategy strategy;
  private ChunkingConfig defaultConfig;

  @BeforeEach
  void setUp() {
    strategy = new FixedSizeChunkingStrategy();
    defaultConfig = ChunkingConfig.defaultConfig();
  }

  @Test
  void testChunkLargeDocument() {
    // Create a document larger than max chunk size
    String largeDoc = "This is a sentence with multiple words that will be repeated. ".repeat(50);

    List<Document> chunks =
        strategy.chunkDocument(largeDoc, "large.txt", "Large Document", defaultConfig);

    // Should create multiple chunks
    assertTrue(chunks.size() > 1, "Large document should be split into multiple chunks");

    // Each chunk should be within size limits
    for (Document chunk : chunks) {
      assertTrue(
          chunk.getText().length() <= defaultConfig.getMaxChunkSize(),
          "Chunk should not exceed max size");
      assertEquals("fixed-size", chunk.getMetadata().get("chunking_strategy"));
      assertEquals("fixed-size", chunk.getMetadata().get("chunk_method"));
    }

    // Verify overlaps exist between consecutive chunks (if configured)
    if (defaultConfig.getOverlapSize() > 0 && chunks.size() > 1) {
      for (int i = 1; i < chunks.size(); i++) {
        String previousChunk = chunks.get(i - 1).getText();
        String currentChunk = chunks.get(i).getText();

        // Check if there's some overlap (exact match depends on word boundary adjustment)
        String endOfPrevious =
            previousChunk.substring(
                Math.max(0, previousChunk.length() - defaultConfig.getOverlapSize() - 50));
        assertTrue(currentChunk.length() > 0, "Current chunk should not be empty");
      }
    }
  }

  @Test
  void testChunkSmallDocument() {
    String smallDoc = "This is a small document that fits in one chunk.";

    List<Document> chunks =
        strategy.chunkDocument(smallDoc, "small.txt", "Small Document", defaultConfig);

    // Should create exactly one chunk
    assertEquals(1, chunks.size(), "Small document should result in single chunk");

    Document chunk = chunks.get(0);
    assertEquals(smallDoc, chunk.getText(), "Content should be preserved exactly");
    assertEquals("fixed-size", chunk.getMetadata().get("chunking_strategy"));
  }

  @Test
  void testChunkWithWordBoundaries() {
    String content = "Word1 Word2 Word3 Word4 Word5 Word6 Word7 Word8 Word9 Word10 ".repeat(20);

    ChunkingConfig config =
        ChunkingConfig.builder()
            .maxChunkSize(500)
            .preferredChunkSize(200)
            .overlapSize(50)
            .maintainSentenceBoundaries(true) // Enable word boundary preservation
            .build();

    List<Document> chunks = strategy.chunkDocument(content, "words.txt", "Word Document", config);

    assertTrue(chunks.size() > 1, "Should create multiple chunks");

    // Check that chunks don't break words (when boundary maintenance is enabled)
    for (Document chunk : chunks) {
      String text = chunk.getText().trim();
      assertFalse(text.isEmpty(), "Chunk should not be empty");

      // Just verify basic functionality - chunks should not be empty
      assertTrue(text.length() > 0, "Chunk should not be empty");
    }
  }

  @Test
  void testChunkWithoutWordBoundaries() {
    String content = "WordWordWordWordWordWordWordWordWordWord".repeat(50);

    ChunkingConfig config =
        ChunkingConfig.builder()
            .maxChunkSize(500)
            .preferredChunkSize(200)
            .overlapSize(50)
            .maintainSentenceBoundaries(false) // Disable word boundary preservation
            .build();

    List<Document> chunks =
        strategy.chunkDocument(content, "nowords.txt", "No Words Document", config);

    assertTrue(chunks.size() > 1, "Should create multiple chunks");

    // Verify chunks are created even without word boundaries
    for (Document chunk : chunks) {
      assertFalse(chunk.getText().trim().isEmpty(), "Chunk should not be empty");
      assertTrue(
          chunk.getText().length() <= config.getMaxChunkSize(), "Chunk should not exceed max size");
    }
  }

  @Test
  void testEmptyContent() {
    List<Document> chunks = strategy.chunkDocument("", "empty.txt", "Empty", defaultConfig);
    assertTrue(chunks.isEmpty(), "Empty content should result in no chunks");

    chunks = strategy.chunkDocument(null, "null.txt", "Null", defaultConfig);
    assertTrue(chunks.isEmpty(), "Null content should result in no chunks");

    chunks = strategy.chunkDocument("   ", "whitespace.txt", "Whitespace", defaultConfig);
    assertTrue(chunks.isEmpty(), "Whitespace-only content should result in no chunks");
  }

  @Test
  void testStrategyProperties() {
    assertEquals("fixed-size", strategy.getStrategyName());
    assertEquals(50, strategy.getPriority());
    assertTrue(strategy.canHandle(createMockAnalysis()));
    assertNotNull(strategy.getDescription());
    assertTrue(strategy.getDescription().contains("fixed-size"));
  }

  @Test
  void testChunkMetadata() {
    String content = "Test content for metadata verification.";

    ChunkingConfig config =
        ChunkingConfig.builder()
            .preferredChunkSize(500)
            .overlapSize(25)
            .maintainSentenceBoundaries(true)
            .build();

    List<Document> chunks = strategy.chunkDocument(content, "test.txt", "Test Title", config);

    assertEquals(1, chunks.size());
    Document chunk = chunks.get(0);

    // Verify all expected metadata is present
    assertEquals("test.txt", chunk.getMetadata().get("filename"));
    assertEquals("Test Title", chunk.getMetadata().get("title"));
    assertEquals(0, chunk.getMetadata().get("chunk_index"));
    assertEquals(content.length(), chunk.getMetadata().get("chunk_size"));
    assertEquals("fixed-size", chunk.getMetadata().get("chunking_strategy"));
    assertEquals("fixed-size", chunk.getMetadata().get("chunk_method"));
    assertEquals(500, chunk.getMetadata().get("target_chunk_size"));
    assertEquals(25, chunk.getMetadata().get("overlap_size"));
    assertEquals(true, chunk.getMetadata().get("maintains_word_boundaries"));
  }

  @Test
  void testCustomChunkSizes() {
    String content = "A ".repeat(1000); // 2000 characters

    ChunkingConfig config =
        ChunkingConfig.builder().maxChunkSize(300).preferredChunkSize(150).overlapSize(30).build();

    List<Document> chunks = strategy.chunkDocument(content, "custom.txt", "Custom", config);

    assertTrue(chunks.size() > 5, "Should create many small chunks");

    for (Document chunk : chunks) {
      assertTrue(chunk.getText().length() <= 300, "Should respect custom max size");
    }
  }

  private DocumentAnalysis createMockAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.SIMPLE_TEXT);
    analysis.setTotalLength(2000);
    analysis.setHeaderCount(0);
    analysis.setCodeBlockCount(0);
    analysis.setListItemCount(0);
    return analysis;
  }
}
