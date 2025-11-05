package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChunkingStrategyRegistryTest {

  private ChunkingStrategyRegistry registry;
  private List<ChunkingStrategy> strategies;

  @BeforeEach
  void setUp() {
    strategies =
        List.of(
            new IntelligentChunkingStrategy(),
            new FixedSizeChunkingStrategy(),
            new RecursiveChunkingStrategy());
    registry = new ChunkingStrategyRegistry(strategies);
  }

  @Test
  void testRegistryInitialization() {
    // Verify all strategies are registered
    List<String> strategyNames = registry.getAvailableStrategyNames();
    assertEquals(3, strategyNames.size());
    assertTrue(strategyNames.contains("intelligent"));
    assertTrue(strategyNames.contains("fixed-size"));
    assertTrue(strategyNames.contains("recursive"));

    // Verify strategies are sorted by priority (intelligent should be first)
    List<ChunkingStrategy> allStrategies = registry.getAllStrategies();
    assertEquals("intelligent", allStrategies.get(0).getStrategyName());
  }

  @Test
  void testFindBestStrategy() {
    // Test for technical document
    DocumentAnalysis technicalAnalysis = createTechnicalAnalysis();
    ChunkingStrategy strategy = registry.findBestStrategy(technicalAnalysis);
    assertEquals("intelligent", strategy.getStrategyName());

    // Test for simple document
    DocumentAnalysis simpleAnalysis = createSimpleAnalysis();
    strategy = registry.findBestStrategy(simpleAnalysis);
    assertNotNull(strategy); // Should find some strategy
  }

  @Test
  void testGetStrategy() {
    // Test valid strategy names
    ChunkingStrategy strategy = registry.getStrategy("intelligent");
    assertEquals("intelligent", strategy.getStrategyName());

    strategy = registry.getStrategy("fixed-size");
    assertEquals("fixed-size", strategy.getStrategyName());

    strategy = registry.getStrategy("recursive");
    assertEquals("recursive", strategy.getStrategyName());

    // Test invalid strategy name
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          registry.getStrategy("non-existent");
        });
  }

  @Test
  void testFindStrategy() {
    // Test valid strategy names
    Optional<ChunkingStrategy> strategy = registry.findStrategy("intelligent");
    assertTrue(strategy.isPresent());
    assertEquals("intelligent", strategy.get().getStrategyName());

    // Test invalid strategy name
    strategy = registry.findStrategy("non-existent");
    assertFalse(strategy.isPresent());
  }

  @Test
  void testSelectStrategy() {
    DocumentAnalysis analysis = createTechnicalAnalysis();

    // Test with explicit strategy name
    ChunkingStrategy strategy = registry.selectStrategy(analysis, "fixed-size");
    assertEquals("fixed-size", strategy.getStrategyName());

    // Test with null strategy name (should use best match)
    strategy = registry.selectStrategy(analysis, null);
    assertEquals("intelligent", strategy.getStrategyName());

    // Test with empty strategy name (should use best match)
    strategy = registry.selectStrategy(analysis, "");
    assertEquals("intelligent", strategy.getStrategyName());

    // Test with invalid strategy name (should fall back to best match)
    strategy = registry.selectStrategy(analysis, "invalid-strategy");
    assertEquals("intelligent", strategy.getStrategyName());
  }

  @Test
  void testHasStrategy() {
    assertTrue(registry.hasStrategy("intelligent"));
    assertTrue(registry.hasStrategy("fixed-size"));
    assertTrue(registry.hasStrategy("recursive"));
    assertFalse(registry.hasStrategy("non-existent"));
  }

  @Test
  void testGetStrategyInfo() {
    List<ChunkingStrategyRegistry.StrategyInfo> infos = registry.getStrategyInfo();
    assertEquals(3, infos.size());

    // Verify info contains expected data
    boolean foundIntelligent =
        infos.stream()
            .anyMatch(info -> info.getName().equals("intelligent") && info.getPriority() == 100);
    assertTrue(foundIntelligent);

    boolean foundFixedSize =
        infos.stream()
            .anyMatch(info -> info.getName().equals("fixed-size") && info.getPriority() == 50);
    assertTrue(foundFixedSize);

    boolean foundRecursive =
        infos.stream()
            .anyMatch(info -> info.getName().equals("recursive") && info.getPriority() == 75);
    assertTrue(foundRecursive);
  }

  @Test
  void testStrategyInfoToString() {
    ChunkingStrategyRegistry.StrategyInfo info =
        new ChunkingStrategyRegistry.StrategyInfo("test", "Test strategy", 100);

    String toString = info.toString();
    assertTrue(toString.contains("test"));
    assertTrue(toString.contains("Test strategy"));
    assertTrue(toString.contains("100"));
  }

  @Test
  void testPriorityOrdering() {
    // Create strategies with different priorities
    List<ChunkingStrategy> testStrategies =
        List.of(
            new TestStrategy("low", 10),
            new TestStrategy("high", 100),
            new TestStrategy("medium", 50));

    ChunkingStrategyRegistry testRegistry = new ChunkingStrategyRegistry(testStrategies);
    List<ChunkingStrategy> ordered = testRegistry.getAllStrategies();

    // Should be ordered by priority (highest first)
    assertEquals("high", ordered.get(0).getStrategyName());
    assertEquals("medium", ordered.get(1).getStrategyName());
    assertEquals("low", ordered.get(2).getStrategyName());
  }

  @Test
  void testDuplicateStrategyNames() {
    // Test behavior with duplicate strategy names (higher priority wins)
    List<ChunkingStrategy> duplicateStrategies =
        List.of(
            new TestStrategy("duplicate", 50),
            new TestStrategy("duplicate", 100), // Higher priority
            new TestStrategy("unique", 25));

    ChunkingStrategyRegistry testRegistry = new ChunkingStrategyRegistry(duplicateStrategies);

    // Should only have 2 strategies (duplicate removed)
    assertEquals(2, testRegistry.getAvailableStrategyNames().size());

    // Should keep the higher priority one
    ChunkingStrategy strategy = testRegistry.getStrategy("duplicate");
    assertEquals(100, strategy.getPriority());
  }

  private DocumentAnalysis createTechnicalAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.TECHNICAL_GUIDE);
    analysis.setTotalLength(2000);
    analysis.setHeaderCount(5);
    analysis.setCodeBlockCount(2);
    analysis.setListItemCount(10);
    analysis.setComplexityScore(75);
    return analysis;
  }

  private DocumentAnalysis createSimpleAnalysis() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.SIMPLE_TEXT);
    analysis.setTotalLength(500);
    analysis.setHeaderCount(0);
    analysis.setCodeBlockCount(0);
    analysis.setListItemCount(0);
    analysis.setComplexityScore(20);
    return analysis;
  }

  // Test strategy for priority testing
  private static class TestStrategy implements ChunkingStrategy {
    private final String name;
    private final int priority;

    public TestStrategy(String name, int priority) {
      this.name = name;
      this.priority = priority;
    }

    @Override
    public String getStrategyName() {
      return name;
    }

    @Override
    public List<org.springframework.ai.document.Document> chunkDocument(
        String content, String filename, String title, ChunkingConfig config) {
      return List.of();
    }

    @Override
    public boolean canHandle(DocumentAnalysis analysis) {
      return true;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }
}
