package com.zalando.rag.service.chunking;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Utility for generating formatted reports from chunking benchmark results. */
@Component
public class BenchmarkReporter {

  /** Generates a formatted report for a single document benchmark. */
  public String generateDocumentReport(ChunkingBenchmark.BenchmarkResult result) {
    StringBuilder report = new StringBuilder();

    report.append("=".repeat(80)).append("\n");
    report.append("CHUNKING BENCHMARK REPORT\n");
    report.append("=".repeat(80)).append("\n");
    report.append("Document: ").append(result.getFilename()).append("\n");
    report.append("Document Type: ").append(result.getAnalysis().getDocumentType()).append("\n");
    report
        .append("Document Length: ")
        .append(result.getAnalysis().getTotalLength())
        .append(" characters\n");
    report.append("Headers: ").append(result.getAnalysis().getHeaderCount()).append("\n");
    report.append("Code Blocks: ").append(result.getAnalysis().getCodeBlockCount()).append("\n");
    report
        .append("Complexity Score: ")
        .append(result.getAnalysis().getComplexityScore())
        .append("\n");
    report.append("\n");

    // Sort strategies by performance (execution time)
    List<ChunkingBenchmark.StrategyBenchmark> sortedResults =
        result.getStrategyResults().stream()
            .filter(ChunkingBenchmark.StrategyBenchmark::isSuccess)
            .sorted(Comparator.comparing(ChunkingBenchmark.StrategyBenchmark::getExecutionTimeMs))
            .toList();

    report.append("STRATEGY PERFORMANCE COMPARISON\n");
    report.append("-".repeat(80)).append("\n");
    report.append(
        String.format(
            "%-15s %-10s %-10s %-10s %-10s %-15s\n",
            "Strategy", "Time(ms)", "Memory(MB)", "Chunks", "Avg Size", "Chunks/sec"));
    report.append("-".repeat(80)).append("\n");

    for (ChunkingBenchmark.StrategyBenchmark strategy : sortedResults) {
      report.append(
          String.format(
              "%-15s %-10d %-10.2f %-10d %-10.0f %-15.1f\n",
              strategy.getStrategyName(),
              strategy.getExecutionTimeMs(),
              strategy.getMemoryUsedBytes() / (1024.0 * 1024.0),
              strategy.getChunkCount(),
              strategy.getAverageChunkSize(),
              strategy.getChunksPerSecond()));
    }

    // Failed strategies
    List<ChunkingBenchmark.StrategyBenchmark> failedResults =
        result.getStrategyResults().stream().filter(r -> !r.isSuccess()).toList();

    if (!failedResults.isEmpty()) {
      report.append("\nFAILED STRATEGIES\n");
      report.append("-".repeat(40)).append("\n");
      for (ChunkingBenchmark.StrategyBenchmark failed : failedResults) {
        report.append(
            String.format("%-15s: %s\n", failed.getStrategyName(), failed.getErrorMessage()));
      }
    }

    // Best strategy details
    if (result.getBestStrategy() != null) {
      report.append("\nRECOMMENDED STRATEGY\n");
      report.append("-".repeat(40)).append("\n");
      ChunkingBenchmark.StrategyBenchmark best = result.getBestStrategy();
      report.append("Strategy: ").append(best.getStrategyName()).append("\n");
      report.append("Execution Time: ").append(best.getExecutionTimeMs()).append(" ms\n");
      report
          .append("Memory Usage: ")
          .append(String.format("%.2f MB", best.getMemoryUsedBytes() / (1024.0 * 1024.0)))
          .append("\n");
      report.append("Chunks Created: ").append(best.getChunkCount()).append("\n");
      report
          .append("Average Chunk Size: ")
          .append(String.format("%.0f characters", best.getAverageChunkSize()))
          .append("\n");
      report
          .append("Chunk Size Range: ")
          .append(best.getMinChunkSize())
          .append(" - ")
          .append(best.getMaxChunkSize())
          .append(" characters\n");
      report
          .append("Processing Rate: ")
          .append(String.format("%.1f chunks/sec", best.getChunksPerSecond()))
          .append("\n");
    }

    report.append("\n").append("=".repeat(80)).append("\n");
    return report.toString();
  }

  /** Generates a formatted report for aggregate benchmark results. */
  public String generateAggregateReport(ChunkingBenchmark.AggregateBenchmarkResult result) {
    StringBuilder report = new StringBuilder();

    report.append("=".repeat(80)).append("\n");
    report.append("AGGREGATE CHUNKING BENCHMARK REPORT\n");
    report.append("=".repeat(80)).append("\n");
    report.append("Total Documents: ").append(result.getDocumentResults().size()).append("\n");
    report.append("\n");

    // Strategy comparison table
    report.append("STRATEGY PERFORMANCE SUMMARY\n");
    report.append("-".repeat(80)).append("\n");
    report.append(
        String.format(
            "%-15s %-12s %-12s %-10s %-12s %-10s\n",
            "Strategy", "Success Rate", "Avg Time(ms)", "Avg Memory", "Avg Chunks", "Avg Rate"));
    report.append("-".repeat(80)).append("\n");

    // Sort by success rate, then by execution time
    result.getStrategyMetrics().entrySet().stream()
        .sorted(
            (a, b) -> {
              double rateCompare =
                  Double.compare(b.getValue().getSuccessRate(), a.getValue().getSuccessRate());
              if (rateCompare != 0) return (int) rateCompare;
              return Double.compare(
                  a.getValue().getAverageExecutionTimeMs(),
                  b.getValue().getAverageExecutionTimeMs());
            })
        .forEach(
            entry -> {
              String strategyName = entry.getKey();
              ChunkingBenchmark.AggregateStrategyMetrics metrics = entry.getValue();

              report.append(
                  String.format(
                      "%-15s %-12.1f%% %-12.1f %-10.2f %-12.1f %-10.1f\n",
                      strategyName,
                      metrics.getSuccessRate() * 100,
                      metrics.getAverageExecutionTimeMs(),
                      metrics.getAverageMemoryUsageMB(),
                      metrics.getAverageChunkCount(),
                      metrics.getAverageChunksPerSecond()));
            });

    // Document type analysis
    Map<DocumentAnalysis.DocumentType, Long> documentTypeCount =
        result.getDocumentResults().stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    r -> r.getAnalysis().getDocumentType(),
                    java.util.stream.Collectors.counting()));

    if (!documentTypeCount.isEmpty()) {
      report.append("\nDOCUMENT TYPE DISTRIBUTION\n");
      report.append("-".repeat(40)).append("\n");
      documentTypeCount.entrySet().stream()
          .sorted(Map.Entry.<DocumentAnalysis.DocumentType, Long>comparingByValue().reversed())
          .forEach(
              entry -> {
                report.append(
                    String.format("%-20s: %d documents\n", entry.getKey(), entry.getValue()));
              });
    }

    // Best performing strategy by document type
    report.append("\nRECOMMENDATIONS BY DOCUMENT TYPE\n");
    report.append("-".repeat(50)).append("\n");
    Map<DocumentAnalysis.DocumentType, String> bestByType =
        result.getDocumentResults().stream()
            .filter(r -> r.getBestStrategy() != null)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    r -> r.getAnalysis().getDocumentType(),
                    java.util.stream.Collectors.groupingBy(
                        r -> r.getBestStrategy().getStrategyName(),
                        java.util.stream.Collectors.counting())))
            .entrySet()
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        entry.getValue().entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("none")));

    bestByType.forEach(
        (docType, strategy) -> {
          report.append(String.format("%-20s: %s\n", docType, strategy));
        });

    report.append("\n").append("=".repeat(80)).append("\n");
    return report.toString();
  }

  /** Generates a concise comparison table for multiple strategies. */
  public String generateComparisonTable(List<ChunkingBenchmark.StrategyBenchmark> strategies) {
    StringBuilder table = new StringBuilder();

    table.append("STRATEGY COMPARISON\n");
    table.append("-".repeat(100)).append("\n");
    table.append(
        String.format(
            "%-15s %-8s %-10s %-8s %-12s %-10s %-10s %-15s\n",
            "Strategy",
            "Success",
            "Time(ms)",
            "Memory",
            "Chunks",
            "Min Size",
            "Max Size",
            "Throughput"));
    table.append("-".repeat(100)).append("\n");

    for (ChunkingBenchmark.StrategyBenchmark strategy : strategies) {
      if (strategy.isSuccess()) {
        table.append(
            String.format(
                "%-15s %-8s %-10d %-8.1f %-12d %-10d %-10d %-15.1f\n",
                strategy.getStrategyName(),
                "✓",
                strategy.getExecutionTimeMs(),
                strategy.getMemoryUsedBytes() / (1024.0 * 1024.0),
                strategy.getChunkCount(),
                strategy.getMinChunkSize(),
                strategy.getMaxChunkSize(),
                strategy.getChunksPerSecond()));
      } else {
        table.append(
            String.format(
                "%-15s %-8s %s\n", strategy.getStrategyName(), "✗", strategy.getErrorMessage()));
      }
    }

    table.append("-".repeat(100)).append("\n");
    return table.toString();
  }

  /** Generates a CSV report for easy analysis in spreadsheet applications. */
  public String generateCSVReport(ChunkingBenchmark.AggregateBenchmarkResult result) {
    StringBuilder csv = new StringBuilder();

    // Headers
    csv.append(
        "Strategy,SuccessRate,AvgTimeMs,AvgMemoryMB,AvgChunks,AvgChunksPerSec,TotalRuns,SuccessfulRuns\n");

    // Data rows
    result
        .getStrategyMetrics()
        .forEach(
            (strategyName, metrics) -> {
              csv.append(
                  String.format(
                      "%s,%.3f,%.2f,%.2f,%.1f,%.2f,%d,%d\n",
                      strategyName,
                      metrics.getSuccessRate(),
                      metrics.getAverageExecutionTimeMs(),
                      metrics.getAverageMemoryUsageMB(),
                      metrics.getAverageChunkCount(),
                      metrics.getAverageChunksPerSecond(),
                      metrics.getTotalRuns(),
                      metrics.getSuccessfulRuns()));
            });

    return csv.toString();
  }
}
