# Code Quality & Standards

This document outlines the code quality standards, formatting rules, and best practices for the Spring AI RAG Service learning platform.

## 🧹 Code Formatting with Spotless

### Overview

This project uses [Spotless](https://github.com/diffplug/spotless) to maintain consistent code formatting across the entire codebase. Spotless automatically formats Java code, build scripts, and documentation files.

### Available Commands

```bash
# Check code formatting (runs automatically during build)
./gradlew checkStyle

# Auto-format all code files
./gradlew formatCode

# Run Spotless directly
./gradlew spotlessCheck  # Check formatting only
./gradlew spotlessApply  # Apply formatting fixes
```

### Formatting Standards

#### Java Code
- **Format**: Google Java Format (version 1.19.2)
- **Import Management**: Automatic removal of unused imports and proper organization
- **Whitespace**: Trailing whitespace removal
- **Line Endings**: Consistent newline handling
- **Toggle Comments**: Support for `// spotless:off` and `// spotless:on` blocks

#### Build Scripts
- **Gradle Files**: Consistent indentation and formatting
- **Property Files**: Proper key-value alignment

#### Documentation
- **Markdown Files**: Trailing whitespace removal and consistent formatting
- **Configuration Files**: Proper indentation and structure

### Integration with Build Process

Spotless is integrated into the build pipeline:

```gradle
// Make build depend on spotless check
tasks.named('build') {
    dependsOn 'spotlessCheck'
}
```

**Important**: The build will fail if code is not properly formatted. Always run `./gradlew formatCode` before committing.

## 📋 Code Style Guidelines

### Java Conventions

#### Class Structure
```java
// Good: Proper class structure
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkingService {

    // Fields first
    private final DocumentAnalysisService documentAnalysisService;
    private final ChunkingStrategyRegistry strategyRegistry;

    // Public methods
    public List<Document> chunkDocument(String content, String filename, String title) {
        // Implementation
    }

    // Private methods
    private ChunkingConfig determineEffectiveConfig(ChunkingConfig explicitConfig) {
        // Implementation
    }
}
```

#### Method Documentation
```java
/**
 * Chunks a document using the specified strategy and configuration.
 *
 * @param content the document content to chunk
 * @param filename the filename for metadata
 * @param title the document title for metadata
 * @param strategyName specific strategy to use (null for auto-selection)
 * @param config chunking configuration (null for default)
 * @return list of document chunks with metadata
 */
public List<Document> chunkDocument(String content, String filename, String title,
                                  String strategyName, ChunkingConfig config) {
    // Implementation
}
```

#### Error Handling
```java
// Good: Proper exception handling with logging
try {
    List<Document> chunks = strategy.chunkDocument(content, filename, title, config);
    log.info("Successfully chunked document '{}' into {} chunks", filename, chunks.size());
    return chunks;
} catch (Exception e) {
    log.error("Error chunking document '{}' with strategy '{}'", filename, strategyName, e);
    throw new ChunkingException("Failed to chunk document: " + filename, e);
}
```

#### Configuration Classes
```java
// Good: Proper configuration with validation
@Data
@Builder
@ConfigurationProperties(prefix = "rag.chunking")
public class ChunkingProperties {

    @NotNull
    @Builder.Default
    private String defaultStrategy = "intelligent";

    @Valid
    @Builder.Default
    private GlobalConfig global = new GlobalConfig();

    @Valid
    private Map<String, ChunkingConfig> strategies = new HashMap<>();

    // Validation methods
    @PostConstruct
    public void validate() {
        if (global.getMaxChunkSize() <= global.getMinChunkSize()) {
            throw new IllegalArgumentException("Max chunk size must be greater than min chunk size");
        }
    }
}
```

### Testing Standards

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private DocumentAnalysisService analysisService;

    @Mock
    private ChunkingStrategyRegistry strategyRegistry;

    @InjectMocks
    private ChunkingService chunkingService;

    @Test
    @DisplayName("Should chunk document using intelligent strategy for technical content")
    void shouldChunkTechnicalDocumentWithIntelligentStrategy() {
        // Given
        String content = "# Technical Guide\n\n```java\ncode block\n```";
        DocumentAnalysis analysis = DocumentAnalysis.builder()
            .documentType(DocumentType.TECHNICAL_GUIDE)
            .build();

        when(analysisService.analyzeDocument(content)).thenReturn(analysis);

        // When
        List<Document> result = chunkingService.chunkDocument(content, "guide.md", "Guide");

        // Then
        assertThat(result).isNotEmpty();
        verify(analysisService).analyzeDocument(content);
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@ActiveProfiles("test")
class ChunkingIntegrationTest {

    @Autowired
    private ChunkingService chunkingService;

    @Test
    @DisplayName("Should process real document end-to-end")
    void shouldProcessDocumentEndToEnd() {
        // Given
        String content = loadTestDocument("sample-technical-guide.md");

        // When
        List<Document> chunks = chunkingService.chunkDocument(content, "guide.md", "Guide");

        // Then
        assertThat(chunks).hasSizeGreaterThan(0);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getText()).isNotBlank();
            assertThat(chunk.getMetadata()).containsKey("filename");
            assertThat(chunk.getMetadata()).containsKey("chunking_strategy");
        });
    }
}
```

## 🔍 Code Review Guidelines

### Pull Request Checklist

Before submitting a pull request:

- [ ] Code is properly formatted (`./gradlew checkStyle` passes)
- [ ] All tests pass (`./gradlew test`)
- [ ] New functionality includes appropriate tests
- [ ] Documentation is updated for significant changes
- [ ] Logging statements are appropriate and meaningful
- [ ] Error handling is comprehensive
- [ ] Performance implications are considered

### Code Review Focus Areas

#### Architecture & Design
- Does the code follow established patterns?
- Are abstractions appropriate and not over-engineered?
- Is the code extensible for future requirements?

#### Functionality
- Does the code solve the intended problem?
- Are edge cases handled appropriately?
- Is the algorithm/approach optimal?

#### Quality & Maintainability
- Is the code readable and self-documenting?
- Are variable and method names descriptive?
- Is the code DRY (Don't Repeat Yourself)?

#### Testing
- Are critical paths covered by tests?
- Are tests independent and reliable?
- Do tests cover both positive and negative scenarios?

## 📊 Quality Metrics

### Code Coverage
```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

**Target**: Maintain >80% code coverage for core business logic.

### Static Analysis

While Spotless handles formatting, consider adding additional static analysis:

```gradle
// Example: PMD for static analysis
plugins {
    id 'pmd'
}

pmd {
    toolVersion = '6.55.0'
    ruleSets = [
        'category/java/errorprone.xml',
        'category/java/bestpractices.xml'
    ]
}
```

### Performance Benchmarking

Use the built-in benchmarking system to track performance:

```java
// Benchmark chunking strategies
@Component
public class ChunkingPerformanceTest {

    @Autowired
    private ChunkingBenchmark benchmark;

    @EventListener(ApplicationReadyEvent.class)
    public void runPerformanceBenchmarks() {
        Map<String, String> testDocuments = loadTestDocuments();
        AggregateBenchmarkResult results = benchmark.benchmarkDocuments(testDocuments, null);

        // Log results for tracking
        results.getStrategyMetrics().forEach((strategy, metrics) -> {
            log.info("Strategy '{}': avg time {}ms, success rate {}%",
                strategy, metrics.getAverageExecutionTimeMs(), metrics.getSuccessRate() * 100);
        });
    }
}
```

## 🚀 Continuous Integration

### GitHub Actions / CI Pipeline

Example CI configuration for code quality:

```yaml
name: Code Quality

on: [push, pull_request]

jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'

    - name: Check code formatting
      run: ./gradlew spotlessCheck

    - name: Run tests with coverage
      run: ./gradlew test jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

### Pre-commit Hooks

Consider setting up pre-commit hooks:

```bash
# .git/hooks/pre-commit
#!/bin/sh
./gradlew spotlessCheck
if [ $? -ne 0 ]; then
    echo "Code formatting check failed. Run './gradlew formatCode' to fix."
    exit 1
fi
```

## 🎯 Best Practices for Learning Platform

### Documentation-Driven Development
- Document architectural decisions (see `sample-adr.md`)
- Maintain clear API documentation
- Include code examples in documentation

### Experiment-Friendly Code
- Make components easily configurable
- Provide clear extension points
- Include comprehensive logging for learning

### Performance Transparency
- Expose performance metrics
- Provide benchmarking capabilities
- Make trade-offs visible and measurable

### Error Handling for Learning
```java
// Good: Educational error messages
catch (ChunkingException e) {
    log.error("Chunking failed for document '{}' using strategy '{}'. " +
              "This might be due to: 1) Document too large, 2) Strategy misconfiguration, " +
              "3) Content parsing issues. Try adjusting chunk size or using a different strategy.",
              filename, strategyName, e);
    throw new ProcessingException("Document processing failed - see logs for learning insights", e);
}
```

## 🔗 References

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Spotless Documentation](https://github.com/diffplug/spotless)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.best-practices)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)