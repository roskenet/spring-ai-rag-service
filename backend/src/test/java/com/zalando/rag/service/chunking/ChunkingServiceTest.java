package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

  @Mock private DocumentAnalysisService documentAnalysisService;

  @Mock private ChunkingStrategyRegistry strategyRegistry;

  @Mock private ChunkingProperties chunkingProperties;

  private ChunkingService chunkingService;
  private IntelligentChunkingStrategy intelligentStrategy;
  private FixedSizeChunkingStrategy fixedSizeStrategy;

  @BeforeEach
  void setUp() {
    chunkingService =
        new ChunkingService(documentAnalysisService, strategyRegistry, chunkingProperties);
    intelligentStrategy = new IntelligentChunkingStrategy();
    fixedSizeStrategy = new FixedSizeChunkingStrategy();
  }

  @Test
  void testChunkTechnicalDocumentWithIntelligentStrategy() {
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

    // Mock analysis
    DocumentAnalysis analysis = createTechnicalDocumentAnalysis();
    when(documentAnalysisService.analyzeDocument(anyString())).thenReturn(analysis);
    when(strategyRegistry.selectStrategy(any(DocumentAnalysis.class), any()))
        .thenReturn(intelligentStrategy);
    when(chunkingProperties.getConfigForDocumentType(any(), any())).thenReturn(null);

    List<Document> chunks =
        chunkingService.chunkDocument(technicalDoc, "api-guidelines.md", "API Design Guidelines");

    // Should create multiple chunks based on structure
    assertTrue(chunks.size() >= 2, "Should create multiple chunks for structured document");

    // Each chunk should have proper metadata
    for (Document chunk : chunks) {
      assertNotNull(chunk.getMetadata().get("filename"));
      assertNotNull(chunk.getMetadata().get("title"));
      assertNotNull(chunk.getMetadata().get("chunk_index"));
      assertNotNull(chunk.getMetadata().get("chunking_strategy"));

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
  void testChunkWithFixedSizeStrategy() {
    String simpleDoc = "This is a simple document without much structure. ".repeat(50);

    DocumentAnalysis analysis = createSimpleDocumentAnalysis();
    when(documentAnalysisService.analyzeDocument(anyString())).thenReturn(analysis);
    when(strategyRegistry.selectStrategy(any(DocumentAnalysis.class), any()))
        .thenReturn(fixedSizeStrategy);
    when(chunkingProperties.getConfigForDocumentType(any(), any())).thenReturn(null);

    List<Document> chunks =
        chunkingService.chunkDocument(simpleDoc, "simple.txt", "Simple Document", "fixed-size");

    assertFalse(chunks.isEmpty(), "Should create at least one chunk");

    // Verify chunk metadata contains strategy information
    for (Document chunk : chunks) {
      assertEquals("fixed-size", chunk.getMetadata().get("chunking_strategy"));
      assertNotNull(chunk.getMetadata().get("chunk_method"));
    }
  }

  @Test
  void testEmptyContent() {
    List<Document> chunks = chunkingService.chunkDocument("", "empty.txt", "Empty Document");
    assertTrue(chunks.isEmpty(), "Empty content should result in no chunks");

    chunks = chunkingService.chunkDocument(null, "null.txt", "Null Document");
    assertTrue(chunks.isEmpty(), "Null content should result in no chunks");
  }

  @Test
  void testGetAvailableStrategies() {
    // Mock strategy registry to return strategy info
    List<ChunkingStrategyRegistry.StrategyInfo> mockStrategies =
        List.of(
            new ChunkingStrategyRegistry.StrategyInfo("intelligent", "Intelligent strategy", 100),
            new ChunkingStrategyRegistry.StrategyInfo("fixed-size", "Fixed size strategy", 50));
    when(strategyRegistry.getStrategyInfo()).thenReturn(mockStrategies);

    List<ChunkingStrategyRegistry.StrategyInfo> strategies =
        chunkingService.getAvailableStrategies();

    assertEquals(2, strategies.size());
    assertTrue(strategies.stream().anyMatch(s -> s.getName().equals("intelligent")));
    assertTrue(strategies.stream().anyMatch(s -> s.getName().equals("fixed-size")));
  }

  @Test
  void testAnalyzeDocument() {
    DocumentAnalysis expectedAnalysis = createTechnicalDocumentAnalysis();
    when(documentAnalysisService.analyzeDocument(anyString())).thenReturn(expectedAnalysis);

    DocumentAnalysis analysis = chunkingService.analyzeDocument("sample content");

    assertNotNull(analysis);
    assertEquals(DocumentAnalysis.DocumentType.TECHNICAL_GUIDE, analysis.getDocumentType());
  }

  @Test
  void testGetRecommendedStrategy() {
    DocumentAnalysis analysis = createTechnicalDocumentAnalysis();
    when(strategyRegistry.findBestStrategy(any())).thenReturn(intelligentStrategy);

    String strategy = chunkingService.getRecommendedStrategy(analysis);

    assertEquals("intelligent", strategy);
  }

  @Test
  void testChunkWithExplicitConfig() {
    String content = "Test content for explicit configuration.";
    ChunkingConfig config =
        ChunkingConfig.builder().maxChunkSize(500).preferredChunkSize(250).overlapSize(50).build();

    DocumentAnalysis analysis = createSimpleDocumentAnalysis();
    when(documentAnalysisService.analyzeDocument(anyString())).thenReturn(analysis);
    when(strategyRegistry.selectStrategy(any(DocumentAnalysis.class), any()))
        .thenReturn(fixedSizeStrategy);

    List<Document> chunks =
        chunkingService.chunkDocument(content, "test.txt", "Test", null, config);

    assertFalse(chunks.isEmpty());
    for (Document chunk : chunks) {
      assertEquals(500, chunk.getMetadata().get("chunk_config_max_size"));
      assertEquals(50, chunk.getMetadata().get("chunk_config_overlap"));
    }
  }

  private DocumentAnalysis createTechnicalDocumentAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.TECHNICAL_GUIDE);
    analysis.setTotalLength(1500);
    analysis.setHeaderCount(5);
    analysis.setCodeBlockCount(1);
    analysis.setListItemCount(4);
    analysis.setComplexityScore(65);
    analysis.setOptimalChunkSize(1200);
    analysis.setCodeRatio(0.3);
    analysis.setStructureRatio(0.4);
    return analysis;
  }

  private DocumentAnalysis createSimpleDocumentAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.SIMPLE_TEXT);
    analysis.setTotalLength(500);
    analysis.setHeaderCount(0);
    analysis.setCodeBlockCount(0);
    analysis.setListItemCount(0);
    analysis.setComplexityScore(20);
    analysis.setOptimalChunkSize(400);
    analysis.setCodeRatio(0.0);
    analysis.setStructureRatio(0.1);
    return analysis;
  }
}
