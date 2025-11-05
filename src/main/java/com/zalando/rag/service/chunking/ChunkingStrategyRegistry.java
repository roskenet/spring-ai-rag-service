package com.zalando.rag.service.chunking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Registry for managing chunking strategies. Automatically discovers all available strategies and
 * provides methods to select the most appropriate one based on document analysis or explicit
 * strategy name.
 */
@Service
@Slf4j
public class ChunkingStrategyRegistry {

  private final List<ChunkingStrategy> strategies;
  private final Map<String, ChunkingStrategy> strategyMap;

  /**
   * Constructor that auto-discovers all available chunking strategies. Strategies are sorted by
   * priority (highest first).
   *
   * @param strategies all available chunking strategy implementations
   */
  public ChunkingStrategyRegistry(List<ChunkingStrategy> strategies) {
    this.strategies =
        strategies.stream()
            .sorted(Comparator.comparingInt(ChunkingStrategy::getPriority).reversed())
            .collect(Collectors.toList());

    this.strategyMap =
        this.strategies.stream()
            .collect(
                Collectors.toMap(
                    ChunkingStrategy::getStrategyName,
                    strategy -> strategy,
                    (existing, replacement) -> {
                      log.warn(
                          "Duplicate strategy name found: {}. Using strategy with higher priority.",
                          existing.getStrategyName());
                      return existing.getPriority() >= replacement.getPriority()
                          ? existing
                          : replacement;
                    }));

    log.info(
        "Initialized chunking strategy registry with {} strategies: {}",
        this.strategies.size(),
        this.strategies.stream()
            .map(ChunkingStrategy::getStrategyName)
            .collect(Collectors.toList()));
  }

  /**
   * Finds the best strategy for a document based on analysis results. Returns the highest priority
   * strategy that can handle the document.
   *
   * @param analysis document analysis results
   * @return the best matching strategy
   * @throws IllegalStateException if no suitable strategy is found
   */
  public ChunkingStrategy findBestStrategy(DocumentAnalysis analysis) {
    log.debug("Finding best strategy for document type: {}", analysis.getDocumentType());

    return strategies.stream()
        .filter(strategy -> strategy.canHandle(analysis))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No suitable chunking strategy found for document type: "
                        + analysis.getDocumentType()));
  }

  /**
   * Gets a specific strategy by name.
   *
   * @param strategyName the name of the strategy to retrieve
   * @return the strategy if found
   * @throws IllegalArgumentException if strategy is not found
   */
  public ChunkingStrategy getStrategy(String strategyName) {
    ChunkingStrategy strategy = strategyMap.get(strategyName);
    if (strategy == null) {
      throw new IllegalArgumentException(
          String.format(
              "Strategy '%s' not found. Available strategies: %s",
              strategyName, getAvailableStrategyNames()));
    }
    return strategy;
  }

  /**
   * Gets a strategy by name, returning empty if not found.
   *
   * @param strategyName the name of the strategy to retrieve
   * @return optional containing the strategy if found
   */
  public Optional<ChunkingStrategy> findStrategy(String strategyName) {
    return Optional.ofNullable(strategyMap.get(strategyName));
  }

  /**
   * Finds a strategy, preferring explicit name but falling back to best match.
   *
   * @param analysis document analysis results
   * @param preferredStrategyName preferred strategy name (can be null)
   * @return the selected strategy
   */
  public ChunkingStrategy selectStrategy(DocumentAnalysis analysis, String preferredStrategyName) {
    if (preferredStrategyName != null && !preferredStrategyName.trim().isEmpty()) {
      Optional<ChunkingStrategy> preferred = findStrategy(preferredStrategyName.trim());
      if (preferred.isPresent()) {
        log.debug("Using explicitly requested strategy: {}", preferredStrategyName);
        return preferred.get();
      } else {
        log.warn(
            "Requested strategy '{}' not found, falling back to best match", preferredStrategyName);
      }
    }

    ChunkingStrategy bestStrategy = findBestStrategy(analysis);
    log.debug(
        "Selected strategy '{}' for document type '{}'",
        bestStrategy.getStrategyName(),
        analysis.getDocumentType());
    return bestStrategy;
  }

  /**
   * Gets all available strategy names.
   *
   * @return list of strategy names
   */
  public List<String> getAvailableStrategyNames() {
    return new ArrayList<>(strategyMap.keySet());
  }

  /**
   * Gets all registered strategies (sorted by priority).
   *
   * @return list of all strategies
   */
  public List<ChunkingStrategy> getAllStrategies() {
    return List.copyOf(strategies);
  }

  /**
   * Gets information about all available strategies.
   *
   * @return list of strategy information
   */
  public List<StrategyInfo> getStrategyInfo() {
    return strategies.stream()
        .map(
            strategy ->
                new StrategyInfo(
                    strategy.getStrategyName(), strategy.getDescription(), strategy.getPriority()))
        .collect(Collectors.toList());
  }

  /**
   * Checks if a strategy exists.
   *
   * @param strategyName strategy name to check
   * @return true if strategy exists
   */
  public boolean hasStrategy(String strategyName) {
    return strategyMap.containsKey(strategyName);
  }

  /** Information about a chunking strategy. */
  public static class StrategyInfo {
    private final String name;
    private final String description;
    private final int priority;

    public StrategyInfo(String name, String description, int priority) {
      this.name = name;
      this.description = description;
      this.priority = priority;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public int getPriority() {
      return priority;
    }

    @Override
    public String toString() {
      return String.format(
          "StrategyInfo{name='%s', priority=%d, description='%s'}", name, priority, description);
    }
  }
}
