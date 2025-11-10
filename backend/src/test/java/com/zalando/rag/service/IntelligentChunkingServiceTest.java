package com.zalando.rag.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zalando.rag.service.chunking.*;
import com.zalando.rag.service.chunking.ChunkingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class IntelligentChunkingServiceTest {

  private ChunkingService chunkingService;

  @BeforeEach
  void setUp() {
    // Set up the new chunking system for the compatibility wrapper
    DocumentAnalysisService analysisService = new DocumentAnalysisService();
    List<ChunkingStrategy> strategies =
        List.of(
            new IntelligentChunkingStrategy(),
            new FixedSizeChunkingStrategy(),
            new RecursiveChunkingStrategy());
    ChunkingStrategyRegistry registry = new ChunkingStrategyRegistry(strategies);
    ChunkingProperties properties = new ChunkingProperties();

    chunkingService =
        new com.zalando.rag.service.chunking.ChunkingService(analysisService, registry, properties);
  }

  @Test
  void testChunkTechnicalDocument() {
    String technicalDoc =
        """
            # API Design Guidelines

            ## Overview
            This document outlines our API design principles and standards.

            ## Authentication
            All APIs must use JWT tokens for authentication.

            ```java
            @RestController
            public class AuthController {
                public ResponseEntity<String> login() {
                    return ResponseEntity.ok("token");
                }
            }
            ```

            ## Error Handling
            APIs should return consistent error responses:

            - 400: Bad Request
            - 401: Unauthorized
            - 404: Not Found
            - 500: Internal Server Error

            ## Versioning
            Use URL path versioning: `/api/v1/users`

            ### Migration Strategy
            1. Deploy new version alongside old
            2. Gradually migrate clients
            3. Deprecate old version after 6 months
            """;

    List<Document> chunks =
        chunkingService.chunkDocument(technicalDoc, "api-guidelines.md", "API Design Guidelines");

    // Should create multiple chunks based on structure
    assertTrue(chunks.size() >= 2, "Should create multiple chunks for structured document");

    // Each chunk should have proper metadata
    for (Document chunk : chunks) {
      assertNotNull(chunk.getMetadata().get("filename"));
      assertNotNull(chunk.getMetadata().get("title"));
      assertNotNull(chunk.getMetadata().get("chunk_index"));

      String content = chunk.getText();
      assertFalse(content.trim().isEmpty(), "Chunk content should not be empty");

      // Chunks should be reasonable size
      assertTrue(content.length() >= 50, "Chunks should have minimum content");
      assertTrue(content.length() <= 2500, "Chunks should not exceed maximum size");
    }

    // Verify that code blocks and their explanations stay together
    boolean foundCodeWithContext =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("```java")
                        && chunk.getText().contains("authentication"));

    assertTrue(foundCodeWithContext, "Code blocks should be kept with their context");
  }

  @Test
  void testChunkADRDocument() {
    String adrDoc =
        """
            # ADR-001: Use PostgreSQL for Primary Database

            ## Status
            Accepted

            ## Context
            We need to choose a database for our new microservices application.
            The requirements include ACID compliance, good performance, and strong ecosystem.

            ## Decision
            We will use PostgreSQL as our primary database.

            ## Rationale
            PostgreSQL provides:
            - Strong ACID guarantees
            - Excellent performance for our use case
            - Rich ecosystem and tooling
            - JSON support for flexible schemas

            ## Consequences
            ### Positive
            - Reliable data consistency
            - Good performance characteristics
            - Strong community support

            ### Negative
            - Learning curve for team members familiar with NoSQL
            - Requires careful schema design
            """;

    List<Document> chunks =
        chunkingService.chunkDocument(adrDoc, "adr-001.md", "ADR-001: Use PostgreSQL");

    // ADR should be chunked appropriately
    assertTrue(chunks.size() >= 1, "ADR should be chunked appropriately");

    // Verify important content is preserved
    boolean hasDecisionSection =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("Decision") || chunk.getText().contains("PostgreSQL"));

    assertTrue(hasDecisionSection, "Important ADR sections should be preserved");
  }

  @Test
  void testChunkSimpleDocument() {
    String simpleDoc =
        "This is a simple document without much structure. "
            + "It should be chunked as a single piece since it's short and simple.";

    List<Document> chunks = chunkingService.chunkDocument(simpleDoc, "simple.md", "Simple Doc");

    // Simple document should result in minimal chunks
    assertFalse(chunks.isEmpty(), "Simple document should produce at least one chunk");

    if (!chunks.isEmpty()) {
      Document chunk = chunks.get(0);
      assertTrue(
          chunk.getText().contains("simple document"), "Chunk should contain original content");
    }
  }

  @Test
  void testSentenceAwareChunking() {
    String longDoc =
        """
            This is the first sentence of a long document. This sentence continues the thought.
            Here we start a new idea that should be preserved together. This completes the idea.

            ## New Section

            This section has multiple sentences. Each sentence should be kept complete.
            We should never break in the middle of a sentence because that destroys meaning.

            ```java
            public class Example {
                public void method() {
                    System.out.println("Code blocks should be preserved as units");
                }
            }
            ```

            The code above demonstrates sentence preservation. This explanation follows the code.
            """;

    List<Document> chunks = chunkingService.chunkDocument(longDoc, "long-doc.md", "Long Document");

    // Verify sentence boundaries are preserved
    for (Document chunk : chunks) {
      String content = chunk.getText();

      // Should not end mid-sentence (unless it's a code block or special case)
      if (content.contains("sentence") && !content.contains("```")) {
        // Most chunks should end with proper punctuation
        assertTrue(
            content.trim().endsWith(".")
                || content.trim().endsWith("?")
                || content.trim().endsWith("!")
                || content.contains("```")
                || // Code blocks are special
                content.trim().endsWith("#"), // Headers are special
            "Chunk should end at sentence boundary: "
                + content.substring(Math.max(0, content.length() - 50)));
      }

      // Code blocks should be preserved as complete units
      if (content.contains("```java")) {
        assertTrue(
            content.contains("System.out.println"), "Code blocks should be preserved complete");
        assertTrue(content.contains("}"), "Code blocks should be preserved complete");
      }
    }
  }
}
