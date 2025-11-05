package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChunkingBenchmarkTest {

  @Mock private ChunkingService chunkingService;

  @Mock private DocumentAnalysisService analysisService;

  private ChunkingStrategyRegistry strategyRegistry;
  private ChunkingBenchmark benchmark;
  private BenchmarkReporter reporter;

  @BeforeEach
  void setUp() {
    // Create real strategies for testing
    List<ChunkingStrategy> strategies =
        List.of(
            new IntelligentChunkingStrategy(),
            new FixedSizeChunkingStrategy(),
            new RecursiveChunkingStrategy());
    strategyRegistry = new ChunkingStrategyRegistry(strategies);

    benchmark = new ChunkingBenchmark(chunkingService, analysisService, strategyRegistry);
    reporter = new BenchmarkReporter();
  }

  @Test
  void testBenchmarkDocument() {
    String content =
        """
            # Test Document
            This is a test document for benchmarking.

            ## Section 1
            Some content here with multiple sentences. This helps test sentence boundaries.

            ```java
            public class Test {
                public void method() {
                    System.out.println("Hello");
                }
            }
            ```

            ## Section 2
            More content to make the document longer and more realistic for testing.
            """;

    // Mock analysis
    DocumentAnalysis analysis = createMockAnalysis();
    when(analysisService.analyzeDocument(anyString())).thenReturn(analysis);

    ChunkingBenchmark.BenchmarkResult result =
        benchmark.benchmarkDocument(content, "test.md", "Test Document");

    // Verify results
    assertNotNull(result);
    assertEquals("test.md", result.getFilename());
    assertEquals(analysis, result.getAnalysis());
    assertTrue(result.getStrategyResults().size() >= 3); // Should test all strategies

    // Check that at least one strategy succeeded
    boolean hasSuccessfulStrategy =
        result.getStrategyResults().stream()
            .anyMatch(ChunkingBenchmark.StrategyBenchmark::isSuccess);
    assertTrue(hasSuccessfulStrategy, "At least one strategy should succeed");

    // Verify best strategy is selected
    assertNotNull(result.getBestStrategy());
    assertTrue(result.getBestStrategy().isSuccess());
  }

  @Test
  void testBenchmarkMultipleDocuments() {
    Map<String, String> documents =
        Map.of(
            "doc1.md", "# Document 1\nShort document for testing.",
            "doc2.md", "# Document 2\nAnother document. ".repeat(100), // Longer document
            "doc3.md", "# Document 3\n```java\ncode here\n```\nTechnical content.");

    Map<String, String> titles =
        Map.of(
            "doc1.md", "Document 1",
            "doc2.md", "Document 2",
            "doc3.md", "Document 3");

    // Mock analysis for each document
    DocumentAnalysis analysis = createMockAnalysis();
    when(analysisService.analyzeDocument(anyString())).thenReturn(analysis);

    ChunkingBenchmark.AggregateBenchmarkResult result =
        benchmark.benchmarkDocuments(documents, titles);

    // Verify aggregate results
    assertNotNull(result);
    assertEquals(3, result.getDocumentResults().size());
    assertFalse(result.getStrategyMetrics().isEmpty());

    // Check strategy metrics
    for (ChunkingBenchmark.AggregateStrategyMetrics metrics :
        result.getStrategyMetrics().values()) {
      assertTrue(metrics.getTotalRuns() > 0);
      assertTrue(metrics.getSuccessRate() >= 0.0 && metrics.getSuccessRate() <= 1.0);
    }
  }

  @Test
  void testBenchmarkStrategyConfig() {
    String content = "Test content for strategy configuration benchmarking.";

    ChunkingConfig customConfig =
        ChunkingConfig.builder().maxChunkSize(500).preferredChunkSize(250).overlapSize(50).build();

    DocumentAnalysis analysis = createMockAnalysis();
    when(analysisService.analyzeDocument(anyString())).thenReturn(analysis);

    ChunkingBenchmark.StrategyBenchmark result =
        benchmark.benchmarkStrategyConfig("fixed-size", customConfig, content, "test.txt", "Test");

    assertNotNull(result);
    assertEquals("fixed-size", result.getStrategyName());
    assertTrue(result.isSuccess());
    assertTrue(result.getExecutionTimeMs() >= 0);
    assertTrue(result.getChunkCount() > 0);
  }

  @Test
  void testBenchmarkReporter() {
    // Create a mock benchmark result
    DocumentAnalysis analysis = createMockAnalysis();
    ChunkingBenchmark.BenchmarkResult result =
        new ChunkingBenchmark.BenchmarkResult("test.md", analysis);

    // Create mock strategy benchmarks
    ChunkingBenchmark.StrategyBenchmark strategy1 =
        new ChunkingBenchmark.StrategyBenchmark("intelligent");
    strategy1.setSuccess(true);
    strategy1.setExecutionTimeMs(100);
    strategy1.setMemoryUsedBytes(1024 * 1024); // 1MB
    strategy1.setChunkCount(5);
    strategy1.setAverageChunkSize(200);
    strategy1.setMinChunkSize(150);
    strategy1.setMaxChunkSize(250);
    strategy1.setChunksPerSecond(50.0);

    ChunkingBenchmark.StrategyBenchmark strategy2 =
        new ChunkingBenchmark.StrategyBenchmark("fixed-size");
    strategy2.setSuccess(true);
    strategy2.setExecutionTimeMs(50);
    strategy2.setMemoryUsedBytes(512 * 1024); // 0.5MB
    strategy2.setChunkCount(3);
    strategy2.setAverageChunkSize(333);
    strategy2.setMinChunkSize(300);
    strategy2.setMaxChunkSize(350);
    strategy2.setChunksPerSecond(60.0);

    result.addStrategyResult(strategy1);
    result.addStrategyResult(strategy2);
    result.setBestStrategy(strategy2); // Fixed-size is faster

    // Test report generation
    String report = reporter.generateDocumentReport(result);

    assertNotNull(report);
    assertTrue(report.contains("CHUNKING BENCHMARK REPORT"));
    assertTrue(report.contains("test.md"));
    assertTrue(report.contains("intelligent"));
    assertTrue(report.contains("fixed-size"));
    assertTrue(report.contains("RECOMMENDED STRATEGY"));
    assertTrue(report.contains("fixed-size")); // Should show fixed-size as best
  }

  @Test
  void testComparisonTable() {
    List<ChunkingBenchmark.StrategyBenchmark> strategies =
        List.of(
            createMockStrategyBenchmark("intelligent", true, 100, 5),
            createMockStrategyBenchmark("fixed-size", true, 50, 3),
            createMockStrategyBenchmark("recursive", false, 0, 0));

    String table = reporter.generateComparisonTable(strategies);

    assertNotNull(table);
    assertTrue(table.contains("STRATEGY COMPARISON"));
    assertTrue(table.contains("intelligent"));
    assertTrue(table.contains("fixed-size"));
    assertTrue(table.contains("recursive"));
    assertTrue(table.contains("✓")); // Success marker
    assertTrue(table.contains("✗")); // Failure marker
  }

  @Test
  void testCSVReport() {
    ChunkingBenchmark.AggregateBenchmarkResult result =
        new ChunkingBenchmark.AggregateBenchmarkResult();

    ChunkingBenchmark.AggregateStrategyMetrics metrics =
        new ChunkingBenchmark.AggregateStrategyMetrics();
    metrics.setTotalRuns(10);
    metrics.setSuccessfulRuns(9);
    metrics.setSuccessRate(0.9);
    metrics.setAverageExecutionTimeMs(75.5);
    metrics.setAverageMemoryUsageMB(1.5);
    metrics.setAverageChunkCount(4.2);
    metrics.setAverageChunksPerSecond(55.7);

    result.addStrategyMetrics("intelligent", metrics);

    String csv = reporter.generateCSVReport(result);

    assertNotNull(csv);

    assertTrue(csv.contains("Strategy,SuccessRate"), "CSV should contain headers");
    assertTrue(csv.contains("intelligent"), "CSV should contain strategy name");

    // Check that numeric values are present - handle both comma and dot as decimal separators
    assertTrue(csv.contains("0,900") || csv.contains("0.900"), "CSV should contain success rate");
    assertTrue(csv.contains("75,50") || csv.contains("75.50"), "CSV should contain execution time");
    assertTrue(csv.contains("1,50") || csv.contains("1.50"), "CSV should contain memory usage");
    assertTrue(
        csv.contains("10") && csv.contains("9"), "CSV should contain total and successful runs");
  }

  private ChunkingBenchmark.StrategyBenchmark createMockStrategyBenchmark(
      String name, boolean success, long timeMs, int chunks) {
    ChunkingBenchmark.StrategyBenchmark benchmark = new ChunkingBenchmark.StrategyBenchmark(name);
    benchmark.setSuccess(success);
    benchmark.setExecutionTimeMs(timeMs);
    benchmark.setMemoryUsedBytes(1024 * 1024);
    benchmark.setChunkCount(chunks);
    benchmark.setAverageChunkSize(200);
    benchmark.setMinChunkSize(150);
    benchmark.setMaxChunkSize(250);
    benchmark.setChunksPerSecond(chunks > 0 ? (double) chunks / (timeMs / 1000.0) : 0.0);
    if (!success) {
      benchmark.setErrorMessage("Mock error for testing");
    }
    return benchmark;
  }

  private DocumentAnalysis createMockAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.TECHNICAL_GUIDE);
    analysis.setTotalLength(1000);
    analysis.setHeaderCount(3);
    analysis.setCodeBlockCount(1);
    analysis.setListItemCount(5);
    analysis.setComplexityScore(60);
    analysis.setOptimalChunkSize(800);
    return analysis;
  }
}
