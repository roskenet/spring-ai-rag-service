# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a RAG (Retrieval-Augmented Generation) platform built with Spring Boot 3.5.7 and Spring AI 1.0.3, designed to demonstrate enterprise Java integration with AI services. The system ingests documents, creates vector embeddings, and provides contextual answers using PostgreSQL with pgvector.

## Architecture

**Backend (Spring Boot)**:
- **Main Package**: `com.zalando.rag` - follows standard Spring layered architecture
- **Controllers**: Handle REST API endpoints (`/api/chat`, `/api/documents`, `/api/analytics`, `/api/config`)
- **Services**: Core business logic including `RagService`, `DocumentIngestionService`, and multiple chunking strategies
- **Entities**: JPA entities for documents, metrics, and configuration
- **Configuration**: Spring AI integration with OpenAI-compatible ZLLM API

**Chunking System**: The core innovation is the pluggable chunking strategy system:
- `ChunkingStrategy` interface with implementations for different document types
- `ChunkingStrategyRegistry` for dynamic strategy selection
- Document analysis service that automatically selects optimal chunking approach
- Configurable via YAML with global defaults and document-type overrides

**Frontend (Next.js)**:
- Material UI components with analytics dashboard
- Chat interface and document upload functionality

**Database**:
- PostgreSQL with pgvector extension for vector storage
- Flyway migrations in `src/main/resources/db/`
- HNSW indexing for efficient semantic search

## Development Commands

### Local Development Setup
```bash
# Start full stack (recommended)
./run-local.sh start

# Start with database admin tools
./run-local.sh admin

# Backend only (requires separate DB)
cd backend && ./gradlew bootRun

# Frontend only (requires backend running)
cd frontend && npm run dev
```

### Backend Development
```bash
cd backend

# Build and test
./gradlew build
./gradlew test

# Code formatting (enforced in build)
./gradlew spotlessCheck
./gradlew spotlessApply

# Run application with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run specific test class
./gradlew test --tests "ChunkingServiceTest"

# Build Docker image
./gradlew jib
```

### Frontend Development
```bash
cd frontend

# Development server
npm run dev

# Build for production
npm run build
npm run start

# Linting
npm run lint
```

### Docker Operations
```bash
# Start everything
./run-local.sh start

# View logs
./run-local.sh logs [service-name]

# Stop and cleanup
./run-local.sh clean

# Service status
./run-local.sh status
```

## Key Configuration

**Environment Variables**:
- `ZTOKEN`: Required for ZLLM API access (Zalando's OpenAI-compatible service)
- `DB_USERNAME/DB_PASSWORD`: Database credentials (defaults to postgres/postgres)
- `SPRING_PROFILES_ACTIVE`: Environment profile (dev/docker/prod)

**Application Configuration**:
The system uses hierarchical configuration in `application.yaml`:
1. Global chunking defaults apply to all strategies
2. Strategy-specific settings override globals
3. Document-type settings override both

Example structure:
```yaml
rag:
  chunking:
    default-strategy: intelligent
    global:
      min-chunk-size: 200
      preserve-code-blocks: true
    strategies:
      intelligent:
        max-chunk-size: 2500
    document-types:
      TECHNICAL_GUIDE:
        max-chunk-size: 3000
```

## API Testing

**Key Endpoints**:
- `POST /api/documents/upload` - Document ingestion
- `POST /api/chat/ask` - RAG-based question answering
- `GET /api/analytics/*` - System metrics and performance data
- `GET /api/config` - Current configuration

**Testing Workflow**:
```bash
# Upload document
curl -X POST http://localhost:8080/api/documents/upload -F "file=@doc.md"

# Ask question
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this about?", "maxResults": 5}'
```

## Code Organization

**Core Services**:
- `RagService`: Main RAG orchestration and query handling
- `DocumentIngestionService`: Document processing and storage
- `IntelligentChunkingService`: Context-aware document chunking
- `AnalyticsService`: Performance metrics collection

**Chunking Strategies** (in `service/chunking/`):
- `IntelligentChunkingStrategy`: AI-aware chunking with structure preservation
- `FixedSizeChunkingStrategy`: Simple size-based chunking
- `RecursiveChunkingStrategy`: Hierarchical document processing
- `CodeAwareChunkingStrategy`: Specialized for technical documentation

**Extensibility**: Add new chunking strategies by implementing `ChunkingStrategy` interface and registering in `ChunkingStrategyRegistry`.

## Database Schema

**Key Tables**:
- `documents`: Document metadata and content
- `vector_store`: Spring AI vector storage (pgvector)
- `query_metrics`, `document_metrics`, `system_metrics`: Analytics data

**Database Tools**:
- pgAdmin available at http://localhost:5050 (admin@example.com/admin)
- Use for exploring vector embeddings and query performance

## Production Considerations

**Build Pipeline**:
- Spotless enforces Google Java Format
- JIB handles containerization with Java agents
- Flyway manages database migrations

**Monitoring**:
- Spring Actuator endpoints enabled
- Comprehensive metrics collection for query performance
- Built-in benchmarking tools for chunking strategy evaluation

**Deployment**:
- Multi-stage Docker builds
- Health checks configured for all services
- Optimized JVM settings for containerized deployment

## Testing Strategy

**Integration Tests**: Focus on chunking strategies and end-to-end RAG workflows
**Unit Tests**: Individual service components and chunking algorithms
**Benchmark Tests**: Performance measurement for different chunking approaches

Example test execution:
```bash
# All tests
./gradlew test

# Chunking-specific tests
./gradlew test --tests "*Chunking*"

# Integration tests only
./gradlew test --tests "*IntegrationTest"
```