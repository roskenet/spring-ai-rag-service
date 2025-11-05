# Development Guide

This guide provides comprehensive information for engineers working with the Spring AI RAG Service learning platform.

## 🚀 Getting Started

### Prerequisites

- **Java 21+**: Required for Spring Boot 3.5.x compatibility
- **Docker & Docker Compose**: For PostgreSQL and pgAdmin setup
- **Zalando Platform Access**: For ZLLM API integration

### Environment Setup

```bash
# Clone the repository
git clone <repository-url>
cd spring-ai-rag-service

# Start PostgreSQL and pgAdmin with Docker Compose
docker-compose up -d

# Configure environment variables
export ZTOKEN="your-zalando-zllm-token"

# Build and run the application
./gradlew bootRun
```

### Quick Validation

```bash
# Upload a technical document
curl -X POST http://localhost:9090/api/documents/upload \
  -F "file=@sample-adr.md"

# Query the knowledge base
curl -X POST http://localhost:9090/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the key architectural decisions?",
    "maxResults": 5,
    "similarityThreshold": 0.7,
    "includeSourceInfo": true
  }'
```

## 🏗️ Architecture Overview

### Core Components

#### Document Processing Pipeline
```
Document Upload → Content Analysis → Strategy Selection → Chunking → Vectorization → Indexing
       ↓              ↓                 ↓              ↓           ↓           ↓
   Validation    Structure Detection  Optimal Config  Intelligent  Embedding  pgvector
   File Type     Headers/Code/Lists   Selection      Boundaries   Generation  Storage
```

#### Key Services

- **DocumentIngestionService**: Handles file upload and processing coordination
- **ChunkingService**: Main facade for document chunking with strategy selection
- **ChunkingStrategyRegistry**: Manages and selects optimal chunking strategies
- **VectorStoreService**: Manages vector storage and similarity search
- **RagService**: Coordinates retrieval and generation for question answering

### Chunking Strategies

#### 1. Intelligent Chunking (`IntelligentChunkingStrategy`)
- **Use Case**: Technical documents with structure (headers, code blocks)
- **Features**: Context-aware segmentation, boundary preservation
- **Configuration**: `rag.chunking.strategies.intelligent`

#### 2. Fixed-Size Chunking (`FixedSizeChunkingStrategy`)
- **Use Case**: Simple text documents, predictable processing
- **Features**: Consistent chunk sizes, memory optimization
- **Configuration**: `rag.chunking.strategies.fixed-size`

#### 3. Recursive Chunking (`RecursiveChunkingStrategy`)
- **Use Case**: Large, hierarchical documents
- **Features**: Hierarchical decomposition, relationship preservation
- **Configuration**: `rag.chunking.strategies.recursive`

#### 4. Code-Aware Chunking (`CodeAwareChunkingStrategy`)
- **Use Case**: API documentation, code-heavy content
- **Features**: Syntax preservation, language detection
- **Configuration**: Custom implementation for specialized needs

## 🔧 Development Workflow

### For Contributors: Code Quality Standards

**Note**: These steps are only required for contributors making code changes. General users can skip this section.

This project uses **Spotless** for consistent formatting and automatically enforces code quality:

```bash
# Before making changes - format your code
./gradlew formatCode

# Check formatting before committing
./gradlew checkStyle

# Run all checks (includes formatting verification)
./gradlew build
```

**Important**: Code formatting is automatically checked during the build process. If you're contributing code, always run `./gradlew formatCode` before committing changes.

### Testing Strategy

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*ChunkingTest*"
./gradlew test --tests "*IntegrationTest*"
```

### Performance Testing

Use the built-in benchmarking system:

```java
@Autowired
private ChunkingBenchmark chunkingBenchmark;

// Benchmark a single document
BenchmarkResult result = chunkingBenchmark.benchmarkDocument(content, filename, title);

// Compare multiple strategies
AggregateBenchmarkResult aggregateResult = chunkingBenchmark.benchmarkDocuments(documents, titles);
```

## 🧪 Experimentation & Learning

### Adding New Chunking Strategies

1. **Implement the Interface**:
```java
@Component
public class CustomChunkingStrategy implements ChunkingStrategy {
    @Override
    public String getStrategyName() { return "custom"; }

    @Override
    public List<Document> chunkDocument(String content, String filename,
                                       String title, ChunkingConfig config) {
        // Your chunking logic here
        return chunks;
    }

    @Override
    public boolean canHandle(DocumentAnalysis analysis) {
        // Define when this strategy should be used
        return analysis.getDocumentType() == DocumentType.CUSTOM;
    }

    @Override
    public int getPriority() {
        return 50; // Higher values take precedence
    }
}
```

2. **Add Configuration**:
```yaml
rag:
  chunking:
    strategies:
      custom:
        max-chunk-size: 1500
        preserve-code-blocks: true
```

3. **Test Your Strategy**:
```java
@Test
void testCustomStrategy() {
    ChunkingConfig config = ChunkingConfig.builder()
        .maxChunkSize(1500)
        .build();

    List<Document> chunks = customStrategy.chunkDocument(content, "test.md", "Test", config);

    assertThat(chunks).isNotEmpty();
    // Add your assertions
}
```

### Document Analysis Extension

Extend the `DocumentAnalysisService` to add custom analysis:

```java
@Service
public class EnhancedDocumentAnalysis extends DocumentAnalysisService {

    public DocumentAnalysis analyzeDocument(String content) {
        DocumentAnalysis baseAnalysis = super.analyzeDocument(content);

        // Add custom analysis
        double complexityScore = calculateComplexity(content);
        String domain = detectDomain(content);

        return baseAnalysis.withCustomMetrics(complexityScore, domain);
    }
}
```

### Configuration Experimentation

#### Document-Type Specific Configuration
```yaml
rag:
  chunking:
    document-types:
      TECHNICAL_GUIDE:
        max-chunk-size: 3000
        preserve-code-blocks: true
        overlap-size: 200
      API_DOCUMENTATION:
        max-chunk-size: 2500
        preserve-code-blocks: true
        maintain-sentence-boundaries: true
```

#### Strategy-Specific Tuning
```yaml
rag:
  chunking:
    strategies:
      intelligent:
        preserve-markdown-structure: true
        section-importance-weighting: true
      code-aware:
        syntax-highlighting-preservation: true
        code-context-expansion: true
```

## 🗄️ Database Exploration

### pgAdmin Access
- **URL**: http://localhost:5050
- **Credentials**: See docker-compose.yml
- **Database**: zeos_rag_dev

### Key Tables and Views

#### Vector Store Schema
```sql
-- Explore vector embeddings
SELECT id,
       metadata->>'filename' as filename,
       metadata->>'chunk_index' as chunk_index,
       metadata->>'chunking_strategy' as strategy,
       length(content) as content_length
FROM vector_store
ORDER BY metadata->>'filename', (metadata->>'chunk_index')::int;

-- Analyze chunking strategy distribution
SELECT metadata->>'chunking_strategy' as strategy,
       COUNT(*) as chunk_count,
       AVG(length(content)) as avg_chunk_size
FROM vector_store
GROUP BY metadata->>'chunking_strategy';
```

#### Document Metadata Analysis
```sql
-- Document processing statistics
SELECT filename,
       status,
       chunk_count,
       file_size,
       created_at,
       updated_at
FROM document
ORDER BY created_at DESC;
```

### Vector Search Analysis

```sql
-- Example similarity search (replace with actual embedding)
SELECT content,
       metadata,
       1 - (embedding <=> '[0.1,0.2,...]'::vector) as similarity
FROM vector_store
ORDER BY embedding <=> '[0.1,0.2,...]'::vector
LIMIT 5;
```

## 🔍 Debugging & Troubleshooting

### Common Issues

#### 1. Chunking Strategy Not Selected
- **Symptom**: Default strategy always used
- **Debug**: Check `DocumentAnalysis.getDocumentType()`
- **Solution**: Verify strategy `canHandle()` implementation

#### 2. Vector Store Connection Issues
- **Symptom**: Embedding storage fails
- **Debug**: Check pgvector extension installation
- **Solution**: Ensure Docker Compose started properly

#### 3. ZLLM API Issues
- **Symptom**: Chat responses fail
- **Debug**: Verify ZTOKEN environment variable
- **Solution**: Check token expiration and permissions

### Logging Configuration

Enable detailed logging for debugging:

```yaml
logging:
  level:
    com.zalando.zeos_rag: DEBUG
    org.springframework.ai: DEBUG
    root: INFO
```

### Performance Monitoring

Monitor chunking performance:

```java
@Autowired
private ChunkingBenchmark benchmark;

// Profile different strategies
BenchmarkResult result = benchmark.benchmarkDocument(content, filename, title);
System.out.println("Strategy: " + result.getBestStrategy().getStrategyName());
System.out.println("Execution time: " + result.getBestStrategy().getExecutionTimeMs() + "ms");
System.out.println("Memory used: " + result.getBestStrategy().getMemoryUsedBytes() + " bytes");
```

## 📈 Learning Objectives

### Hands-On Experiments

1. **Document Type Analysis**:
   - Upload different document types (technical guides, APIs, simple text)
   - Observe automatic strategy selection
   - Compare chunking results in pgAdmin

2. **Strategy Comparison**:
   - Process same document with different strategies
   - Analyze chunk sizes and boundaries
   - Measure performance differences

3. **Configuration Tuning**:
   - Experiment with chunk sizes
   - Test overlap settings
   - Observe retrieval quality changes

4. **Vector Search Behavior**:
   - Try different similarity thresholds
   - Vary result counts
   - Understand embedding quality impact

### Extension Opportunities

- **AWS Bedrock Integration**: Direct Knowledge Base connector
- **Multi-Vector Store Support**: Redis, Pinecone, Weaviate
- **Advanced Analytics**: Query patterns, content optimization
- **Custom Document Types**: Domain-specific processing
- **Multilingual Support**: Language-aware chunking

## 🔗 Related Documentation

- [API Reference](./API.md) - Complete REST API documentation
- [Code Quality Guide](./CODE_QUALITY.md) - Formatting and standards
- [Architecture Decision Records](../sample-adr.md) - Example ADR format
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)