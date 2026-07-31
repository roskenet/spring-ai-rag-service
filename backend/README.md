# ZEOS RAG Backend

Spring Boot backend service for the ZEOS RAG (Retrieval-Augmented Generation) platform.

## Architecture

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Database**: PostgreSQL with pgvector extension
- **Vector Store**: Spring AI PgVector integration
- **Build Tool**: Gradle
- **Migration**: Flyway

## Features

- **RAG Implementation**: Retrieval-Augmented Generation using Spring AI
- **Vector Search**: Semantic search using PostgreSQL pgvector
- **Document Processing**: Multiple chunking strategies (intelligent, code-aware, recursive, fixed-size)
- **Analytics**: Comprehensive metrics collection and analytics
- **REST APIs**: Full CRUD operations for documents, chat, and configuration

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL with pgvector extension
- Docker (optional)

### Database Setup

1. Start PostgreSQL with pgvector:
```bash
docker-compose up -d
```

2. The database will be automatically initialized using Flyway migrations.

### Running the Application

```bash
# Using Gradle wrapper
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/spring-ai-rag-service-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:9090`

### Configuration

Set the following environment variables:

```bash
export AWS_REGION=eu-central-1
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

Or use the `.env.example` file as a template.

## API Endpoints

### Chat
- `POST /api/chat/ask` - General chat with RAG
- `POST /api/chat/ask/{documentId}` - Chat with specific document

### Documents
- `POST /api/documents/upload` - Upload documents
- `GET /api/documents` - List documents
- `DELETE /api/documents/{id}` - Delete document

### Analytics
- `GET /api/analytics/dashboard` - Dashboard summary
- `GET /api/analytics/queries` - Query analytics
- `GET /api/analytics/documents` - Document analytics
- `GET /api/analytics/system` - System metrics

### Configuration
- `GET /api/config` - Get current configuration
- `POST /api/config` - Update configuration

## Metrics Collection

The system tracks:
- **Query Metrics**: Response time, accuracy, success rate
- **Document Metrics**: Processing time, chunk counts, access patterns
- **System Metrics**: Total latency, error rates, token usage

## Chunking Strategies

1. **Intelligent**: Context-aware chunking with sentence boundaries
2. **Code-aware**: Preserves code blocks and markdown structure
3. **Recursive**: Hierarchical chunking for large documents
4. **Fixed-size**: Simple fixed-size chunks with overlap

## Database Schema

The application uses Flyway for database migrations. See `src/main/resources/db/` for migration scripts.

Key tables:
- `documents` - Document metadata and content
- `vector_store` - Embeddings and vector search
- `query_metrics` - Query performance data
- `document_metrics` - Document processing metrics
- `system_metrics` - System performance data

## Development

### Code Style
The project uses Spotless for code formatting:

```bash
# Check code style
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessApply
```

### Testing
```bash
./gradlew test
```

## Configuration Options

See `src/main/resources/application.yaml` for detailed configuration options including:
- Spring AI model settings
- Vector store configuration
- Chunking strategy parameters
- Database connection settings
