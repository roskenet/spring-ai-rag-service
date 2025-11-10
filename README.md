# ZEOS RAG Platform: Advanced Learning Platform

> **A comprehensive monorepo platform for Retrieval-Augmented Generation (RAG) architecture featuring both Spring Boot backend and Next.js frontend, designed to build deep technical knowledge and hands-on experience with modern AI integration patterns.**

## Mission Statement

This project serves as a sophisticated learning platform for RAG architecture within the Spring ecosystem, specifically designed for engineers to gain hands-on experience with modern AI integration patterns. Built to accelerate knowledge building in the AI domain, this platform demonstrates advanced chunking strategies, vectorization techniques, and extensible patterns that provide a solid foundation for future production implementations.

## 🏗️ Engineering Philosophy

As engineers building expertise in AI systems, we recognize that RAG implementations require thoughtful consideration of:

- **Semantic Chunking Strategy**: Moving beyond naive fixed-size splitting to intelligent, context-aware document segmentation
- **Vector Store Design**: Leveraging PostgreSQL with pgvector for enterprise-grade persistence and scalability
- **Extensible Strategy Pattern**: Future-proofing the system for integration with AWS Bedrock Knowledge Bases and other vector stores
- **Performance Engineering**: Built-in benchmarking and metrics for informed technical decisions

## Key Features

### Advanced Chunking Architecture

This implementation showcases **four distinct chunking strategies**, each optimized for different document types and use cases:

#### 1. Intelligent Chunking Strategy (`IntelligentChunkingStrategy`)
- **Context-Aware Segmentation**: Analyzes document structure (headers, code blocks, lists) to create semantically meaningful chunks
- **Boundary Preservation**: Maintains sentence boundaries and code block integrity
- **Adaptive Sizing**: Dynamic chunk sizing based on content complexity and document type
- **Metadata Enrichment**: Captures structural metadata for enhanced retrieval relevance

```java
// Supports intelligent merging of small sections while preserving context
ChunkingConfig.technicalConfig()
    .withPreserveCodeBlocks(true)
    .withMaintainSentenceBoundaries(true)
    .withMaxChunkSize(2500);
```

#### 2. Fixed-Size Chunking Strategy (`FixedSizeChunkingStrategy`)
- **Predictable Performance**: Consistent chunk sizes for benchmarking and testing
- **Memory Optimization**: Controlled memory footprint for resource-constrained environments
- **Baseline Implementation**: Reference implementation for strategy comparisons

#### 3. Recursive Chunking Strategy (`RecursiveChunkingStrategy`)
- **Hierarchical Decomposition**: Recursive splitting based on document structure
- **Preserves Relationships**: Maintains parent-child relationships in hierarchical documents
- **Configurable Depth**: Adjustable recursion levels for different document complexities

#### 4. Code-Aware Chunking Strategy (`CodeAwareChunkingStrategy`)
- **Syntax Preservation**: Specialized handling of technical documentation with code examples
- **Language Detection**: Automatic identification and preservation of code blocks by language
- **API Documentation Optimization**: Tailored for technical specifications and API docs

### Vector Store Integration with pgvector

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW           # Hierarchical Navigable Small World for optimal search
        distance-type: COSINE_DISTANCE
        dimensions: 1536           # OpenAI embedding dimensions
        initialize-schema: true    # Automatic schema management
```

### Extensible Configuration Framework

The system employs a sophisticated configuration hierarchy that supports:

- **Global Defaults**: Base configuration applied across all strategies
- **Strategy-Specific Overrides**: Fine-tuned parameters per chunking approach
- **Document-Type Optimization**: Specialized configurations for different content types
- **Environment-Specific Profiles**: Dev, staging, and production configurations

```yaml
rag:
  chunking:
    default-strategy: intelligent
    global:
      min-chunk-size: 200
      max-chunk-size: 2000
      preserve-code-blocks: true
      maintain-sentence-boundaries: true
    strategies:
      intelligent:
        max-chunk-size: 2500
        preserve-markdown-structure: true
    document-types:
      TECHNICAL_GUIDE:
        max-chunk-size: 3000
        overlap-size: 200
```

### Performance Engineering & Benchmarking

The `ChunkingBenchmark` service provides comprehensive performance analysis:

- **Strategy Comparison**: Side-by-side performance metrics across all strategies
- **Memory Profiling**: Heap usage tracking for memory-sensitive deployments
- **Throughput Analysis**: Characters/second and chunks/second measurements
- **Quality Metrics**: Chunk size distribution and variance analysis

```java
BenchmarkResult result = chunkingBenchmark.benchmarkDocument(content, filename, title);
// Automatic selection of optimal strategy based on performance characteristics
```

## Technology Stack

### Core Framework
- **Spring Boot 3.5.7**: Latest enterprise framework with native compilation support
- **Spring AI 1.0.3**: Cutting-edge AI integration framework
- **Java 21**: Modern language features and performance optimizations

### AI & Vector Processing
- **ZLLM API**: Zalando's OpenAI-compatible API backed by AWS Bedrock for language model access
- **GPT-4o via ZLLM**: State-of-the-art language model for question answering
- **text-embedding-3-small**: High-quality embeddings with 1536 dimensions
- **pgvector**: PostgreSQL extension for high-performance vector operations

### Data Persistence
- **PostgreSQL**: Enterprise-grade RDBMS with vector extension support
- **Spring Data JPA**: Object-relational mapping with query optimization
- **HNSW Indexing**: Hierarchical Navigable Small World for sub-linear search complexity

## Learning Objectives

This platform serves as a comprehensive educational tool for understanding:

### RAG Architecture Fundamentals
- **Document Ingestion Pipeline**: Multi-stage processing from upload to vectorization
- **Semantic Search Implementation**: Cosine similarity with configurable thresholds
- **Context Assembly**: Intelligent context window management for optimal LLM performance

### Advanced Chunking Techniques
- **Structure-Aware Processing**: Header hierarchy preservation and code block isolation
- **Adaptive Sizing**: Dynamic chunk sizing based on content analysis
- **Overlap Strategy**: Intelligent overlap calculation to maintain context continuity

### Production Readiness Patterns
- **Error Handling**: Comprehensive exception management with graceful degradation
- **Monitoring & Observability**: Built-in metrics and performance tracking
- **Configuration Management**: Environment-specific configuration with validation

### Spring AI Integration Patterns
- **Advisor Pattern**: Extensible processing pipeline for document transformation
- **Vector Store Abstraction**: Database-agnostic vector operations
- **Chat Model Integration**: Seamless LLM communication with prompt template management

## Quick Start Guide

Get up and running in minutes with Docker automation for the complete application stack.

### Prerequisites
- **Docker** and **Docker Compose** installed
- **`ztoken` CLI tool** installed (recommended), OR
- **ZTOKEN** environment variable with your JWT token

### One-Command Setup

```bash
# Simply run the application stack - token will be fetched automatically
./run-local.sh start
```

The script automatically:
- ✅ **Fetches JWT token** using the `ztoken` CLI tool (if available)
- ✅ **Falls back** to existing `ZTOKEN` environment variable
- ✅ **Provides clear guidance** if neither option is available

That's it! The script will:
- ✅ Build backend and frontend Docker images
- ✅ Start PostgreSQL with pgvector extension
- ✅ Deploy backend Spring Boot application
- ✅ Deploy frontend Next.js application
- ✅ Wait for all services to be healthy
- ✅ Display access URLs

### Access Your Application

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | Main application interface |
| **Backend API** | http://localhost:8080 | REST API and health endpoints |
| **PgAdmin** | http://localhost:5050 | Database admin (admin@example.com / admin) |

### Script Commands

```bash
# Start all services
./run-local.sh start

# Start with database admin interface
./run-local.sh admin

# View service status
./run-local.sh status

# View logs (all services)
./run-local.sh logs

# View logs for specific service
./run-local.sh logs backend
./run-local.sh logs frontend

# Stop all services
./run-local.sh stop

# Restart all services
./run-local.sh restart

# Clean up (stop and remove volumes)
./run-local.sh clean

# Show help
./run-local.sh help
```

### Test the System

```bash
# Test the API
curl -X POST http://localhost:8080/api/documents/upload -F "file=@sample-adr.md"

# Or use the frontend interface at http://localhost:3000
```

### Manual Development Setup (Alternative)

If you prefer to run services individually for development:

#### 1. Start Database Only

```bash
cd backend
docker-compose up -d
```

#### 2. Start Backend (Development Mode)

```bash
cd backend
# Token will be fetched automatically if ztoken CLI is available
export ZTOKEN=$(ztoken 2>/dev/null || echo "your-jwt-token-here")
./gradlew bootRun
```

#### 3. Start Frontend (Development Mode)

```bash
cd frontend
npm install
npm run dev
```

### Troubleshooting

#### Common Issues

**Token issues:**
```bash
# If ztoken CLI is not available and ZTOKEN not set
export ZTOKEN="your-jwt-token-here"
./run-local.sh start

# Or install the ztoken CLI tool and run:
./run-local.sh start
```

**Port conflicts:**
```bash
# If ports 3000, 8080, or 5432 are in use
./run-local.sh stop
# Or modify ports in docker-compose.yml
```

**Docker build issues:**
```bash
# Clean up and rebuild
./run-local.sh clean
docker system prune -f
./run-local.sh start
```

**Service health check failures:**
```bash
# Check service logs
./run-local.sh logs backend
./run-local.sh logs frontend

# Restart specific service
docker-compose restart backend
```

#### System Requirements

- **Memory**: Minimum 4GB RAM (8GB recommended)
- **Disk**: At least 5GB free space for Docker images
- **Network**: Internet access for downloading dependencies

#### Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `ZTOKEN` | No* | JWT token for backend authentication | `eyJ0eXAiOiJKV1QiLCJhbGc...` |
| `NEXT_PUBLIC_API_URL` | No | Frontend API URL (auto-configured) | `http://localhost:8080` |

_*Required only if `ztoken` CLI tool is not available_

#### Token Fetching Options

1. **Automatic (Recommended)**: Install `ztoken` CLI tool - tokens fetched automatically
2. **Manual**: Set `ZTOKEN` environment variable manually
3. **Override**: Use `ZTOKEN='token' ./run-local.sh start` to override automatic fetching

## 📊 Architecture Deep Dive
## Architecture Deep Dive

Explore the sophisticated architecture behind this RAG learning platform.

**→ [Detailed Architecture Guide](docs/DEVELOPMENT.md#architecture-overview)**

### Key Architectural Highlights

- **Intelligent Document Processing**: Automatic strategy selection based on content analysis
- **Extensible Chunking Framework**: Four different strategies with easy extensibility
- **Vector Store Integration**: PostgreSQL with pgvector for production-grade vector operations
- **Performance Benchmarking**: Built-in tools for comparing and optimizing strategies

**→ [Database Schema & Exploration Guide](docs/DEVELOPMENT.md#database-exploration)**

## Configuration & Customization

The platform provides extensive configuration options for different document types and use cases.

**→ [Complete Configuration Guide](docs/DEVELOPMENT.md#configuration-experimentation)**

### Configuration Highlights

- **Strategy-Specific Settings**: Fine-tune each chunking approach
- **Document-Type Optimization**: Specialized configurations for technical docs, APIs, and simple text
- **Performance Tuning**: Adjustable chunk sizes, overlap, and processing options
- **Extension Points**: Easy customization for specific use cases

## Performance & Benchmarking

Understand and measure the performance characteristics of different chunking strategies.

**→ [Performance Testing Guide](docs/DEVELOPMENT.md#performance-testing)**

### Built-in Benchmarking

The platform includes comprehensive benchmarking tools to:
- Compare chunking strategies side-by-side
- Measure memory usage and processing speed
- Analyze chunk quality and distribution
- Guide optimization decisions

**→ [Benchmarking API Documentation](docs/DEVELOPMENT.md#performance-monitoring)**

## Roadmap & Extensibility

### Phase 1: AWS Bedrock Integration
- **Knowledge Base Connector**: Direct integration with AWS Bedrock KB
- **Multi-Vector Store Support**: Abstraction layer for different vector databases
- **Hybrid Search**: Combination of semantic and keyword search

### Phase 2: Advanced Analytics
- **Query Analytics**: Understanding user interaction patterns
- **Content Optimization**: Automatic chunk quality assessment
- **Performance Monitoring**: Real-time metrics and alerting

### Phase 3: Production-Ready Features
- **Multi-Tenancy**: Isolated document collections per tenant (for production extension)
- **Access Control**: Fine-grained permissions and document security
- **Audit Logging**: Comprehensive tracking for compliance requirements

## Code Quality & Standards

**For Contributors**: Maintain high code quality with automated formatting and comprehensive guidelines.

**→ [Complete Code Quality Guide](docs/CODE_QUALITY.md)**

### Key Quality Features

- **Automated Formatting**: Spotless integration with Google Java Format enforced at build time
- **Build Integration**: Quality checks run automatically during build process
- **Comprehensive Standards**: Guidelines for Java, testing, and documentation
- **CI/CD Ready**: Pre-configured for continuous integration

**→ [Detailed Formatting Rules & Standards](docs/CODE_QUALITY.md#formatting-standards)**

## Documentation

Comprehensive documentation for all aspects of the platform:

| Document | Description |
|----------|-------------|
| **[Development Guide](docs/DEVELOPMENT.md)** | Complete setup, architecture, and development workflow |
| **[API Reference](docs/API.md)** | REST API endpoints, examples, and testing guides |
| **[Code Quality](docs/CODE_QUALITY.md)** | Formatting standards, best practices, and quality tools |

### Quick Navigation

- **Getting Started**: [Development Setup](docs/DEVELOPMENT.md#getting-started)
- **API Usage**: [API Examples](docs/API.md#testing-with-curl)
- **Architecture**: [System Design](docs/DEVELOPMENT.md#architecture-overview)
- **Extensions**: [Adding Strategies](docs/DEVELOPMENT.md#adding-new-chunking-strategies)
- **Database**: [pgAdmin & Queries](docs/DEVELOPMENT.md#database-exploration)

## Contributing & Learning

This platform is designed for collaborative learning and extension.

**→ [Complete Extension Guide](docs/DEVELOPMENT.md#experimentation--learning)**

### Learning Objectives

- **RAG Architecture**: Understand document processing, vectorization, and retrieval
- **Spring AI Integration**: Learn modern AI framework patterns
- **Chunking Strategies**: Explore different approaches to document segmentation
- **Vector Databases**: Work with pgvector and similarity search
- **Performance Analysis**: Use built-in benchmarking for optimization

## Additional Resources

### Spring AI Documentation
- [Spring AI Reference Guide](https://docs.spring.io/spring-ai/reference/)
- [Vector Store Configuration](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
- [Document Readers](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)

### RAG Architecture Patterns
- [Enterprise RAG Implementation Guide](https://example.com/rag-guide)
- [Vector Database Optimization](https://example.com/vector-optimization)
- [Chunking Strategy Best Practices](https://example.com/chunking-best-practices)

### Performance Tuning
- [pgvector Performance Guide](https://github.com/pgvector/pgvector#performance)
- [Spring AI Configuration Tuning](https://docs.spring.io/spring-ai/reference/configuration/)

## API Reference

**→ [Complete API Documentation](docs/API.md)**

Includes RESTful endpoints, request/response schemas, cURL examples, and comprehensive testing guides.

---

**Built with engineering excellence for hands-on learning in the AI domain.**

*This project demonstrates the intersection of traditional enterprise patterns with cutting-edge AI capabilities, providing a comprehensive learning foundation for RAG implementations in the Spring ecosystem. Extend and adapt this platform as you build expertise toward production-ready systems.*
