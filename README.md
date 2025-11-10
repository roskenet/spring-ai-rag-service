# ZEOS RAG Platform

> **A RAG implementation built with Spring AI to help engineering teams understand modern AI integration patterns. We're exploring how Java ecosystems can work alongside the dominant Python ML stack.**

## Why This Exists

Most companies struggle with a fundamental problem when adopting LLMs: these models know everything about the internet but nothing about your specific business. You ask GPT about your company's remote work policy, and it gives you generic advice instead of your actual HR guidelines.

RAG solves this by letting you augment the LLM's response with your own data. The architecture is straightforward: chunk your documents, convert them to vector embeddings, store them in a database, then retrieve relevant chunks when users ask questions.

This project shows you how to do this properly in a Spring Boot environment, with some thoughtful engineering around chunking strategies and performance monitoring.

## Technical Approach

We built this on Spring Boot 3.5.7 with Spring AI 1.0.3 because:
- Most enterprise Java teams already know this stack
- Spring AI handles the plumbing between your app and various AI services
- You get familiar patterns for configuration, testing, and deployment
- Integration with existing enterprise infrastructure is straightforward

Key decisions:
- **PostgreSQL + pgvector**: Reliable, ACID-compliant vector storage that your ops team already knows how to manage
- **Multiple chunking strategies**: Because one size doesn't fit all document types
- **Built-in benchmarking**: So you can measure what actually works for your data
- **Docker everything**: One-command local development setup

## Understanding RAG

Here's what happens when someone asks a question:

```
1. User asks: "What's our policy on remote work?"
2. Convert question to vector embedding
3. Search vector database for similar content
4. Retrieve relevant document chunks
5. Send chunks + question to LLM
6. LLM responds with context-aware answer
```

The tricky parts are getting the chunking right (so you preserve context) and tuning the similarity search (so you retrieve relevant but not redundant information).

## Spring AI vs Python ML Stack

Let's be honest: Python dominates machine learning. PyTorch, Hugging Face, LangChain - the ecosystem is mature and well-supported. We're not trying to replace that.

But if you're building production applications, especially in enterprise environments, Spring offers some advantages:
- Your Java teams can contribute without learning Python
- Enterprise security and compliance patterns are well-established
- Operational monitoring and deployment are mature
- Configuration management is robust

Think of this as expanding your toolkit, not replacing it. Use Python for model development and research. Use Spring AI when you need to integrate AI capabilities into production Java applications.

## Chunking Strategies

We implemented four different approaches because documents vary widely:

**Intelligent Chunking**: Analyzes document structure (headers, code blocks, lists) and tries to preserve semantic boundaries. Works well for technical documentation.

**Fixed-Size Chunking**: Simple approach that splits text into consistent chunks. Predictable performance, useful for baseline comparisons.

**Recursive Chunking**: Splits hierarchically, maintaining relationships between sections. Good for structured documents.

**Code-Aware Chunking**: Handles technical docs with code examples properly, preserving syntax highlighting and code block integrity.

Each strategy is configurable:

```yaml
rag:
  chunking:
    default-strategy: intelligent
    strategies:
      intelligent:
        max-chunk-size: 2500
        preserve-code-blocks: true
      fixed-size:
        max-chunk-size: 1000
```

## Quick Start

If you have Docker installed:

```bash
./run-local.sh start
```

This starts PostgreSQL with pgvector, the Spring Boot backend, and a Next.js frontend. The script handles token management automatically if you have the `ztoken` CLI installed.

Access points:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Database admin: http://localhost:5050

Test it:
```bash
curl -X POST http://localhost:8080/api/documents/upload -F "file=@your-doc.md"
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"message": "What does the document say about X?"}'
```

## Configuration

The configuration system is hierarchical:

1. **Global defaults** apply to all strategies
2. **Strategy-specific** settings override globals
3. **Document-type** settings override both

This lets you fine-tune behavior without duplicating configuration:

```yaml
rag:
  chunking:
    global:
      min-chunk-size: 200
      preserve-code-blocks: true
    document-types:
      TECHNICAL_GUIDE:
        max-chunk-size: 3000
        overlap-size: 200
```

## Performance and Monitoring

We included benchmarking tools because performance matters:

```java
BenchmarkResult result = chunkingBenchmark.benchmarkDocument(content, filename, title);
```

This measures:
- Processing speed (characters/sec)
- Memory usage patterns
- Chunk size distribution
- Quality metrics

The frontend includes an analytics dashboard showing query performance, system health, and usage patterns.

## Technology Stack

**Backend**:
- Spring Boot 3.5.7 with Spring AI 1.0.3
- Java 21 (for modern language features)
- PostgreSQL with pgvector extension
- ZLLM API (Zalando's OpenAI-compatible service)

**Frontend**:
- Next.js with TypeScript
- Material UI for components
- Recharts for analytics visualization

**Infrastructure**:
- Docker Compose for local development
- Automated health checks and dependency management

## Extending the Platform

Adding a new chunking strategy:

```java
@Component
public class MyChunkingStrategy implements ChunkingStrategy {
    @Override
    public List<Document> chunk(String content, ChunkingConfig config) {
        // Your implementation here
        return chunks;
    }
}
```

Register it:

```java
@PostConstruct
public void registerStrategies() {
    registry.register("my-strategy", new MyChunkingStrategy());
}
```

## What's Next

Some areas we're considering:

**Integration improvements**:
- AWS Bedrock Knowledge Bases support
- Multiple vector store backends
- Hybrid semantic + keyword search

**Production features**:
- Multi-tenancy support
- Fine-grained access controls
- Audit logging for compliance

**Analytics enhancements**:
- Query pattern analysis
- Automatic configuration tuning
- Content quality assessment

## Documentation

- **[Development Guide](docs/DEVELOPMENT.md)**: Setup instructions and architecture details
- **[API Reference](docs/API.md)**: REST endpoints and examples
- **[Code Quality](docs/CODE_QUALITY.md)**: Standards and formatting rules

## Learning Resources

**Spring AI**:
- [Official Documentation](https://docs.spring.io/spring-ai/reference/)
- [Vector Store Integration](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)

**RAG Architecture**:
- [pgvector Performance Guide](https://github.com/pgvector/pgvector#performance)

---

This project demonstrates practical RAG implementation patterns within familiar Java tooling. It's designed for learning and experimentation, with enough production considerations to be useful as a foundation for real applications.

The goal is to help Java teams understand modern AI integration without requiring a complete shift to Python-based tooling. Extend it, modify it, and adapt it to your specific use cases.