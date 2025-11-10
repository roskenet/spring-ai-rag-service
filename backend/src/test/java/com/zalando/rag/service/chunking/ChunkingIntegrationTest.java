package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * Integration tests for the complete chunking system, testing the interaction between all
 * components: DocumentAnalysisService, ChunkingStrategyRegistry, ChunkingService, and various
 * strategies.
 */
class ChunkingIntegrationTest {

  private ChunkingService chunkingService;
  private DocumentAnalysisService analysisService;
  private ChunkingStrategyRegistry registry;
  private ChunkingProperties properties;

  @BeforeEach
  void setUp() {
    // Set up all components
    analysisService = new DocumentAnalysisService();

    List<ChunkingStrategy> strategies =
        List.of(
            new IntelligentChunkingStrategy(),
            new FixedSizeChunkingStrategy(),
            new RecursiveChunkingStrategy());
    registry = new ChunkingStrategyRegistry(strategies);

    properties = new ChunkingProperties();
    // Set up default properties
    properties.setDefaultStrategy("intelligent");

    chunkingService = new ChunkingService(analysisService, registry, properties);
  }

  @Test
  void testEndToEndTechnicalDocument() {
    String technicalDoc =
        """
            # Microservices Architecture Guide

            ## Introduction
            This guide covers the design and implementation of microservices architecture
            for our platform. We'll explore key patterns, best practices, and common pitfalls.

            ## Service Design Principles

            ### Single Responsibility
            Each microservice should have a single, well-defined responsibility.
            This ensures high cohesion and loose coupling between services.

            ### API-First Design
            Design your APIs before implementing the service logic.

            ```yaml
            openapi: 3.0.0
            info:
              title: User Service API
              version: 1.0.0
            paths:
              /users:
                get:
                  summary: Get all users
                  responses:
                    '200':
                      description: List of users
            ```

            ## Communication Patterns

            ### Synchronous Communication
            Use HTTP/REST for synchronous communication between services.
            This is suitable for real-time operations where immediate response is required.

            ```java
            @RestController
            public class UserController {
                @Autowired
                private UserService userService;

                @GetMapping("/users/{id}")
                public ResponseEntity<User> getUser(@PathVariable Long id) {
                    User user = userService.findById(id);
                    return ResponseEntity.ok(user);
                }
            }
            ```

            ### Asynchronous Communication
            Use message queues (RabbitMQ, Kafka) for asynchronous communication.
            This improves system resilience and decoupling.

            ## Data Management

            ### Database per Service
            Each microservice should have its own database to ensure data independence.

            ### Event Sourcing
            Consider event sourcing for services that require audit trails.

            ## Deployment and Operations

            ### Containerization
            Use Docker containers for consistent deployment across environments.

            ### Service Discovery
            Implement service discovery for dynamic service location.

            ### Monitoring and Logging
            Implement comprehensive monitoring and centralized logging.

            ## Conclusion
            Microservices architecture provides scalability and flexibility,
            but requires careful design and operational excellence.
            """;

    // Test automatic strategy selection
    List<Document> chunks =
        chunkingService.chunkDocument(
            technicalDoc, "microservices-guide.md", "Microservices Guide");

    // Verify chunking results
    assertFalse(chunks.isEmpty(), "Should produce chunks");
    assertTrue(chunks.size() >= 3, "Complex document should produce multiple chunks");

    // Verify all chunks have proper metadata
    for (Document chunk : chunks) {
      assertNotNull(chunk.getMetadata().get("filename"));
      assertNotNull(chunk.getMetadata().get("title"));
      assertNotNull(chunk.getMetadata().get("chunking_strategy"));
      assertNotNull(chunk.getMetadata().get("document_type"));
      assertNotNull(chunk.getMetadata().get("chunk_index"));

      // Should use intelligent strategy for technical content
      assertEquals("intelligent", chunk.getMetadata().get("chunking_strategy"));
      // Document type classification might vary, but should be present and valid
      String docType = (String) chunk.getMetadata().get("document_type");
      assertNotNull(docType, "Document type should be present");
      assertTrue(
          docType.equals("TECHNICAL_GUIDE") || docType.equals("GENERAL_DOC"),
          "Document type should be valid: " + docType);
    }

    // Verify code blocks are preserved
    boolean foundCompleteCodeBlock =
        chunks.stream()
            .anyMatch(
                chunk -> {
                  String text = chunk.getText();
                  return text.contains("```yaml")
                      && text.contains("openapi: 3.0.0")
                      && text.contains("```");
                });
    assertTrue(foundCompleteCodeBlock, "YAML code block should be preserved as complete unit");

    boolean foundJavaCodeBlock =
        chunks.stream()
            .anyMatch(
                chunk -> {
                  String text = chunk.getText();
                  return text.contains("```java")
                      && text.contains("@RestController")
                      && text.contains("```");
                });
    assertTrue(foundJavaCodeBlock, "Java code block should be preserved as complete unit");
  }

  @Test
  void testStrategySelection() {
    String simpleText =
        "This is a simple text document without any special structure. ".repeat(100);

    // Test explicit strategy selection
    List<Document> intelligentChunks =
        chunkingService.chunkDocument(simpleText, "simple.txt", "Simple Text", "intelligent");
    List<Document> fixedSizeChunks =
        chunkingService.chunkDocument(simpleText, "simple.txt", "Simple Text", "fixed-size");
    List<Document> recursiveChunks =
        chunkingService.chunkDocument(simpleText, "simple.txt", "Simple Text", "recursive");

    // All should produce chunks but potentially different numbers
    assertFalse(intelligentChunks.isEmpty());
    assertFalse(fixedSizeChunks.isEmpty());
    assertFalse(recursiveChunks.isEmpty());

    // Verify each used the correct strategy
    assertEquals("intelligent", intelligentChunks.get(0).getMetadata().get("chunking_strategy"));
    assertEquals("fixed-size", fixedSizeChunks.get(0).getMetadata().get("chunking_strategy"));
    assertEquals("recursive", recursiveChunks.get(0).getMetadata().get("chunking_strategy"));
  }

  @Test
  void testCustomConfiguration() {
    String content = "Word ".repeat(1000); // 5000 characters

    ChunkingConfig customConfig =
        ChunkingConfig.builder()
            .maxChunkSize(500)
            .preferredChunkSize(250)
            .overlapSize(50)
            .maintainSentenceBoundaries(false)
            .build();

    List<Document> chunks =
        chunkingService.chunkDocument(
            content, "custom.txt", "Custom Config", "fixed-size", customConfig);

    assertTrue(chunks.size() > 5, "Should create many small chunks with custom config");

    for (Document chunk : chunks) {
      assertTrue(chunk.getText().length() <= 500, "Should respect custom max chunk size");
      assertEquals(500, chunk.getMetadata().get("chunk_config_max_size"));
      assertEquals(50, chunk.getMetadata().get("chunk_config_overlap"));
    }
  }

  @Test
  void testBatchChunking() {
    Map<String, String> documents =
        Map.of(
            "doc1.md", "# Document 1\nThis is the first document.",
            "doc2.md", "# Document 2\nThis is the second document.",
            "doc3.md", "# Document 3\nThis is the third document.");

    Map<String, String> titles =
        Map.of(
            "doc1.md", "First Document",
            "doc2.md", "Second Document",
            "doc3.md", "Third Document");

    Map<String, List<Document>> results =
        chunkingService.chunkDocuments(documents, titles, "intelligent", null);

    assertEquals(3, results.size());
    assertTrue(results.containsKey("doc1.md"));
    assertTrue(results.containsKey("doc2.md"));
    assertTrue(results.containsKey("doc3.md"));

    // Each document should have at least one chunk
    for (Map.Entry<String, List<Document>> entry : results.entrySet()) {
      assertFalse(entry.getValue().isEmpty(), "Each document should produce at least one chunk");

      // Verify metadata
      Document firstChunk = entry.getValue().get(0);
      assertEquals(entry.getKey(), firstChunk.getMetadata().get("filename"));
      assertEquals("intelligent", firstChunk.getMetadata().get("chunking_strategy"));
    }
  }

  @Test
  void testDocumentAnalysisIntegration() {
    String apiDoc =
        """
            # REST API Documentation

            ## Authentication Endpoints

            ### POST /auth/login
            Authenticate user and return JWT token.

            ```json
            {
              "username": "user@example.com",
              "password": "securePassword"
            }
            ```

            ### GET /auth/refresh
            Refresh existing JWT token.

            ## User Endpoints

            ### GET /users
            Get list of all users.

            ### POST /users
            Create a new user.
            """;

    // Test document analysis
    DocumentAnalysis analysis = chunkingService.analyzeDocument(apiDoc);
    assertEquals(DocumentAnalysis.DocumentType.API_DOCUMENTATION, analysis.getDocumentType());
    assertTrue(analysis.getHeaderCount() >= 5);
    assertTrue(analysis.getCodeBlockCount() >= 1);

    // Test recommended strategy
    String recommendedStrategy = chunkingService.getRecommendedStrategy(analysis);
    assertEquals("intelligent", recommendedStrategy);

    // Test chunking with analysis results
    List<Document> chunks =
        chunkingService.chunkDocument(apiDoc, "api-doc.md", "API Documentation");

    assertFalse(chunks.isEmpty());
    for (Document chunk : chunks) {
      assertEquals("API_DOCUMENTATION", chunk.getMetadata().get("document_type"));
      assertTrue((Integer) chunk.getMetadata().get("document_complexity") > 30);
    }
  }

  @Test
  void testErrorHandling() {
    // Test with null content
    List<Document> chunks = chunkingService.chunkDocument(null, "null.txt", "Null");
    assertTrue(chunks.isEmpty());

    // Test with empty content
    chunks = chunkingService.chunkDocument("", "empty.txt", "Empty");
    assertTrue(chunks.isEmpty());

    // Test with invalid strategy name (should fall back to best match)
    chunks = chunkingService.chunkDocument("Valid content", "test.txt", "Test", "invalid-strategy");
    assertFalse(chunks.isEmpty());
    assertNotNull(chunks.get(0).getMetadata().get("chunking_strategy"));
  }

  @Test
  void testDifferentDocumentTypes() {
    // Test simple text
    String simpleText = "This is simple text without structure.";
    List<Document> simpleChunks = chunkingService.chunkDocument(simpleText, "simple.txt", "Simple");

    // Test code-heavy document
    String codeDoc =
        """
            ```python
            def hello_world():
                print("Hello, World!")
                return "success"
            ```

            ```javascript
            function helloWorld() {
                console.log("Hello, World!");
                return "success";
            }
            ```

            ```java
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            ```
            """;
    List<Document> codeChunks = chunkingService.chunkDocument(codeDoc, "code.md", "Code Examples");

    // Verify different document types are handled appropriately
    assertFalse(simpleChunks.isEmpty());
    assertFalse(codeChunks.isEmpty());

    // Code document should be recognized as code-heavy or technical
    String codeDocType = (String) codeChunks.get(0).getMetadata().get("document_type");
    assertTrue(codeDocType.equals("CODE_HEAVY") || codeDocType.equals("TECHNICAL_GUIDE"));
  }
}
