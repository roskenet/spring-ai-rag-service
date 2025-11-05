package com.zalando.rag.service.chunking;

import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Utility for benchmarking different chunking strategies to help select the optimal strategy for
 * different document types and use cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkingBenchmark {

  private final ChunkingService chunkingService;
  private final DocumentAnalysisService analysisService;
  private final ChunkingStrategyRegistry strategyRegistry;

  /** Benchmarks all available strategies on a given document. */
  public BenchmarkResult benchmarkDocument(String content, String filename, String title) {
    log.info("Starting benchmark for document: {}", filename);

    DocumentAnalysis analysis = analysisService.analyzeDocument(content);
    BenchmarkResult result = new BenchmarkResult(filename, analysis);

    // Test each available strategy
    for (ChunkingStrategy strategy : strategyRegistry.getAllStrategies()) {
      if (strategy.canHandle(analysis)) {
        StrategyBenchmark strategyResult =
            benchmarkStrategy(strategy, content, filename, title, analysis);
        result.addStrategyResult(strategyResult);
      }
    }

    // Determine best strategy
    result.setBestStrategy(findBestStrategy(result.getStrategyResults()));

    log.info(
        "Benchmark completed for document: {}. Best strategy: {}",
        filename,
        result.getBestStrategy().getStrategyName());

    return result;
  }

  /** Benchmarks multiple documents to get aggregate performance metrics. */
  public AggregateBenchmarkResult benchmarkDocuments(
      Map<String, String> documents, Map<String, String> titles) {
    log.info("Starting aggregate benchmark for {} documents", documents.size());

    AggregateBenchmarkResult aggregateResult = new AggregateBenchmarkResult();
    Map<String, List<StrategyBenchmark>> strategyResults = new HashMap<>();

    for (Map.Entry<String, String> entry : documents.entrySet()) {
      String filename = entry.getKey();
      String content = entry.getValue();
      String title = titles != null ? titles.get(filename) : filename;

      BenchmarkResult docResult = benchmarkDocument(content, filename, title);
      aggregateResult.addDocumentResult(docResult);

      // Collect strategy results for aggregation
      for (StrategyBenchmark strategyResult : docResult.getStrategyResults()) {
        strategyResults
            .computeIfAbsent(strategyResult.getStrategyName(), k -> new ArrayList<>())
            .add(strategyResult);
      }
    }

    // Calculate aggregate metrics
    for (Map.Entry<String, List<StrategyBenchmark>> entry : strategyResults.entrySet()) {
      String strategyName = entry.getKey();
      List<StrategyBenchmark> results = entry.getValue();

      AggregateStrategyMetrics metrics = calculateAggregateMetrics(results);
      aggregateResult.addStrategyMetrics(strategyName, metrics);
    }

    log.info("Aggregate benchmark completed for {} documents", documents.size());
    return aggregateResult;
  }

  /** Benchmarks a specific strategy configuration. */
  public StrategyBenchmark benchmarkStrategyConfig(
      String strategyName, ChunkingConfig config, String content, String filename, String title) {
    ChunkingStrategy strategy = strategyRegistry.getStrategy(strategyName);
    DocumentAnalysis analysis = analysisService.analyzeDocument(content);

    return benchmarkStrategy(strategy, content, filename, title, analysis, config);
  }

  private StrategyBenchmark benchmarkStrategy(
      ChunkingStrategy strategy,
      String content,
      String filename,
      String title,
      DocumentAnalysis analysis) {
    ChunkingConfig defaultConfig = getDefaultConfig(analysis.getDocumentType());
    return benchmarkStrategy(strategy, content, filename, title, analysis, defaultConfig);
  }

  private StrategyBenchmark benchmarkStrategy(
      ChunkingStrategy strategy,
      String content,
      String filename,
      String title,
      DocumentAnalysis analysis,
      ChunkingConfig config) {
    log.debug("Benchmarking strategy: {} for document: {}", strategy.getStrategyName(), filename);

    StrategyBenchmark benchmark = new StrategyBenchmark(strategy.getStrategyName());

    // Warm up (to avoid JIT compilation effects)
    try {
      strategy.chunkDocument(content, filename, title, config);
    } catch (Exception e) {
      log.warn("Warmup failed for strategy {}: {}", strategy.getStrategyName(), e.getMessage());
    }

    // Actual benchmark
    long startTime = System.nanoTime();
    long startMemory = getUsedMemory();

    try {
      List<Document> chunks = strategy.chunkDocument(content, filename, title, config);

      long endTime = System.nanoTime();
      long endMemory = getUsedMemory();

      // Calculate metrics
      long executionTime = endTime - startTime;
      long memoryUsed = Math.max(0, endMemory - startMemory);

      benchmark.setExecutionTimeNanos(executionTime);
      benchmark.setExecutionTimeMs(TimeUnit.NANOSECONDS.toMillis(executionTime));
      benchmark.setMemoryUsedBytes(memoryUsed);
      benchmark.setChunkCount(chunks.size());
      benchmark.setSuccess(true);

      // Calculate chunk statistics
      calculateChunkStatistics(benchmark, chunks);

      // Calculate efficiency metrics
      benchmark.setChunksPerSecond(calculateChunksPerSecond(chunks.size(), executionTime));
      benchmark.setCharactersPerSecond(
          calculateCharactersPerSecond(content.length(), executionTime));

    } catch (Exception e) {
      log.error("Error benchmarking strategy {}: {}", strategy.getStrategyName(), e.getMessage());
      benchmark.setSuccess(false);
      benchmark.setErrorMessage(e.getMessage());
    }

    return benchmark;
  }

  private void calculateChunkStatistics(StrategyBenchmark benchmark, List<Document> chunks) {
    if (chunks.isEmpty()) {
      return;
    }

    int totalSize = 0;
    int minSize = Integer.MAX_VALUE;
    int maxSize = 0;
    List<Integer> sizes = new ArrayList<>();

    for (Document chunk : chunks) {
      int size = chunk.getText().length();
      totalSize += size;
      minSize = Math.min(minSize, size);
      maxSize = Math.max(maxSize, size);
      sizes.add(size);
    }

    benchmark.setAverageChunkSize(totalSize / chunks.size());
    benchmark.setMinChunkSize(minSize);
    benchmark.setMaxChunkSize(maxSize);

    // Calculate standard deviation
    double mean = (double) totalSize / chunks.size();
    double variance =
        sizes.stream().mapToDouble(size -> Math.pow(size - mean, 2)).average().orElse(0.0);
    benchmark.setChunkSizeStdDev(Math.sqrt(variance));
  }

  private double calculateChunksPerSecond(int chunkCount, long executionTimeNanos) {
    if (executionTimeNanos == 0) return 0.0;
    return (double) chunkCount / (executionTimeNanos / 1_000_000_000.0);
  }

  private double calculateCharactersPerSecond(int characterCount, long executionTimeNanos) {
    if (executionTimeNanos == 0) return 0.0;
    return (double) characterCount / (executionTimeNanos / 1_000_000_000.0);
  }

  private StrategyBenchmark findBestStrategy(List<StrategyBenchmark> results) {
    return results.stream()
        .filter(StrategyBenchmark::isSuccess)
        .min(Comparator.comparing(this::calculateScore))
        .orElse(null);
  }

  private double calculateScore(StrategyBenchmark benchmark) {
    // Scoring formula: lower is better
    // Factors: execution time (70%), memory usage (20%), chunk count variability (10%)
    double timeScore = benchmark.getExecutionTimeMs() / 1000.0; // Normalize to seconds
    double memoryScore = benchmark.getMemoryUsedBytes() / (1024.0 * 1024.0); // Normalize to MB
    double variabilityScore =
        benchmark.getChunkSizeStdDev()
            / benchmark.getAverageChunkSize(); // Coefficient of variation

    return (timeScore * 0.7) + (memoryScore * 0.2) + (variabilityScore * 0.1);
  }

  private AggregateStrategyMetrics calculateAggregateMetrics(List<StrategyBenchmark> results) {
    List<StrategyBenchmark> successful =
        results.stream().filter(StrategyBenchmark::isSuccess).toList();

    if (successful.isEmpty()) {
      return new AggregateStrategyMetrics();
    }

    AggregateStrategyMetrics metrics = new AggregateStrategyMetrics();
    metrics.setTotalRuns(results.size());
    metrics.setSuccessfulRuns(successful.size());
    metrics.setSuccessRate((double) successful.size() / results.size());

    // Calculate averages
    metrics.setAverageExecutionTimeMs(
        successful.stream().mapToLong(StrategyBenchmark::getExecutionTimeMs).average().orElse(0.0));

    metrics.setAverageMemoryUsageMB(
        successful.stream().mapToLong(StrategyBenchmark::getMemoryUsedBytes).average().orElse(0.0)
            / (1024.0 * 1024.0));

    metrics.setAverageChunkCount(
        successful.stream().mapToInt(StrategyBenchmark::getChunkCount).average().orElse(0.0));

    metrics.setAverageChunksPerSecond(
        successful.stream()
            .mapToDouble(StrategyBenchmark::getChunksPerSecond)
            .average()
            .orElse(0.0));

    return metrics;
  }

  private ChunkingConfig getDefaultConfig(DocumentAnalysis.DocumentType documentType) {
    switch (documentType) {
      case TECHNICAL_GUIDE:
      case CODE_HEAVY:
      case API_DOCUMENTATION:
        return ChunkingConfig.technicalConfig();
      case COMPREHENSIVE_DOC:
        return ChunkingConfig.largeChunkConfig();
      case SIMPLE_TEXT:
        return ChunkingConfig.simpleConfig();
      default:
        return ChunkingConfig.defaultConfig();
    }
  }

  private long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  /** Result of benchmarking a single document. */
  public static class BenchmarkResult {
    private final String filename;
    private final DocumentAnalysis analysis;
    private final List<StrategyBenchmark> strategyResults = new ArrayList<>();
    private StrategyBenchmark bestStrategy;

    public BenchmarkResult(String filename, DocumentAnalysis analysis) {
      this.filename = filename;
      this.analysis = analysis;
    }

    public void addStrategyResult(StrategyBenchmark result) {
      strategyResults.add(result);
    }

    // Getters
    public String getFilename() {
      return filename;
    }

    public DocumentAnalysis getAnalysis() {
      return analysis;
    }

    public List<StrategyBenchmark> getStrategyResults() {
      return strategyResults;
    }

    public StrategyBenchmark getBestStrategy() {
      return bestStrategy;
    }

    public void setBestStrategy(StrategyBenchmark bestStrategy) {
      this.bestStrategy = bestStrategy;
    }
  }

  /** Benchmark results for a specific strategy. */
  public static class StrategyBenchmark {
    private final String strategyName;
    private boolean success;
    private String errorMessage;
    private long executionTimeNanos;
    private long executionTimeMs;
    private long memoryUsedBytes;
    private int chunkCount;
    private double averageChunkSize;
    private int minChunkSize;
    private int maxChunkSize;
    private double chunkSizeStdDev;
    private double chunksPerSecond;
    private double charactersPerSecond;

    public StrategyBenchmark(String strategyName) {
      this.strategyName = strategyName;
    }

    // Getters and setters
    public String getStrategyName() {
      return strategyName;
    }

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    public long getExecutionTimeNanos() {
      return executionTimeNanos;
    }

    public void setExecutionTimeNanos(long executionTimeNanos) {
      this.executionTimeNanos = executionTimeNanos;
    }

    public long getExecutionTimeMs() {
      return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
      this.executionTimeMs = executionTimeMs;
    }

    public long getMemoryUsedBytes() {
      return memoryUsedBytes;
    }

    public void setMemoryUsedBytes(long memoryUsedBytes) {
      this.memoryUsedBytes = memoryUsedBytes;
    }

    public int getChunkCount() {
      return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
      this.chunkCount = chunkCount;
    }

    public double getAverageChunkSize() {
      return averageChunkSize;
    }

    public void setAverageChunkSize(double averageChunkSize) {
      this.averageChunkSize = averageChunkSize;
    }

    public int getMinChunkSize() {
      return minChunkSize;
    }

    public void setMinChunkSize(int minChunkSize) {
      this.minChunkSize = minChunkSize;
    }

    public int getMaxChunkSize() {
      return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
      this.maxChunkSize = maxChunkSize;
    }

    public double getChunkSizeStdDev() {
      return chunkSizeStdDev;
    }

    public void setChunkSizeStdDev(double chunkSizeStdDev) {
      this.chunkSizeStdDev = chunkSizeStdDev;
    }

    public double getChunksPerSecond() {
      return chunksPerSecond;
    }

    public void setChunksPerSecond(double chunksPerSecond) {
      this.chunksPerSecond = chunksPerSecond;
    }

    public double getCharactersPerSecond() {
      return charactersPerSecond;
    }

    public void setCharactersPerSecond(double charactersPerSecond) {
      this.charactersPerSecond = charactersPerSecond;
    }
  }

  /** Aggregate benchmark results across multiple documents. */
  public static class AggregateBenchmarkResult {
    private final List<BenchmarkResult> documentResults = new ArrayList<>();
    private final Map<String, AggregateStrategyMetrics> strategyMetrics = new HashMap<>();

    public void addDocumentResult(BenchmarkResult result) {
      documentResults.add(result);
    }

    public void addStrategyMetrics(String strategyName, AggregateStrategyMetrics metrics) {
      strategyMetrics.put(strategyName, metrics);
    }

    public List<BenchmarkResult> getDocumentResults() {
      return documentResults;
    }

    public Map<String, AggregateStrategyMetrics> getStrategyMetrics() {
      return strategyMetrics;
    }
  }

  /** Aggregate metrics for a strategy across multiple documents. */
  public static class AggregateStrategyMetrics {
    private int totalRuns;
    private int successfulRuns;
    private double successRate;
    private double averageExecutionTimeMs;
    private double averageMemoryUsageMB;
    private double averageChunkCount;
    private double averageChunksPerSecond;

    // Getters and setters
    public int getTotalRuns() {
      return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
      this.totalRuns = totalRuns;
    }

    public int getSuccessfulRuns() {
      return successfulRuns;
    }

    public void setSuccessfulRuns(int successfulRuns) {
      this.successfulRuns = successfulRuns;
    }

    public double getSuccessRate() {
      return successRate;
    }

    public void setSuccessRate(double successRate) {
      this.successRate = successRate;
    }

    public double getAverageExecutionTimeMs() {
      return averageExecutionTimeMs;
    }

    public void setAverageExecutionTimeMs(double averageExecutionTimeMs) {
      this.averageExecutionTimeMs = averageExecutionTimeMs;
    }

    public double getAverageMemoryUsageMB() {
      return averageMemoryUsageMB;
    }

    public void setAverageMemoryUsageMB(double averageMemoryUsageMB) {
      this.averageMemoryUsageMB = averageMemoryUsageMB;
    }

    public double getAverageChunkCount() {
      return averageChunkCount;
    }

    public void setAverageChunkCount(double averageChunkCount) {
      this.averageChunkCount = averageChunkCount;
    }

    public double getAverageChunksPerSecond() {
      return averageChunksPerSecond;
    }

    public void setAverageChunksPerSecond(double averageChunksPerSecond) {
      this.averageChunksPerSecond = averageChunksPerSecond;
    }
  }
}
