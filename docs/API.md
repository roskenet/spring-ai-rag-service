# Spring AI RAG Service API Documentation

## Overview

The Spring AI RAG Service provides REST endpoints for exploring document ingestion, vectorization, and question-answering using Retrieval-Augmented Generation (RAG). This API enables hands-on experimentation with different chunking strategies, vector search techniques, and Spring AI integration patterns.

## Prerequisites

Before using the API, ensure you have:
- Started the PostgreSQL database and pgAdmin using `docker-compose up -d`
- Set the `ZTOKEN` environment variable with your Zalando ZLLM API token
- Started the application with `./gradlew bootRun`

**Database Access**: pgAdmin is available at `http://localhost:5050` for exploring the vector store and document metadata (credentials in docker-compose.yml).

## Base URL
```
http://localhost:9090
```

## Endpoints

### Document Management

#### Upload Document
```http
POST /api/documents/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (markdown, md, txt files)
```

**Example Response:**
```json
{
  "documentId": 1,
  "filename": "example.md",
  "message": "Document uploaded successfully and processing started",
  "success": true,
  "status": "UPLOADED"
}
```

**Note**: The system automatically analyzes uploaded documents and selects the optimal chunking strategy:
- **Intelligent Strategy**: For technical documents with headers, code blocks, and structured content
- **Fixed-Size Strategy**: For simple text documents
- **Recursive Strategy**: For large, hierarchical documents
- **Code-Aware Strategy**: For API documentation and code-heavy content

#### Get All Documents
```http
GET /api/documents
```

**Example Response:**
```json
[
  {
    "id": 1,
    "filename": "example.md",
    "title": "Example Document",
    "fileSize": 1024,
    "chunkCount": 5,
    "status": "PROCESSED",
    "createdAt": "2025-11-03T17:00:00",
    "updatedAt": "2025-11-03T17:01:00"
  }
]
```

#### Get Document by ID
```http
GET /api/documents/{id}
```

#### Delete Document
```http
DELETE /api/documents/{id}
```

#### Get Documents by Status
```http
GET /api/documents/status/{status}

Status values: UPLOADED, PROCESSING, PROCESSED, FAILED
```

### Chat & RAG

#### Ask Question (Global Search)
```http
POST /api/chat/ask
Content-Type: application/json

{
  "question": "What is the main topic of the documents?",
  "maxResults": 5,
  "similarityThreshold": 0.7,
  "includeSourceInfo": true
}
```

**Example Response:**
```json
{
  "question": "What is the main topic of the documents?",
  "answer": "Based on the provided documents, the main topic is...",
  "sources": [
    {
      "filename": "example.md",
      "title": "Example Document",
      "content": "Content snippet...",
      "similarity": 0.85,
      "chunkIndex": 0
    }
  ],
  "responseTimeMs": 1500
}
```

#### Ask Question About Specific Document
```http
POST /api/chat/ask/{documentId}
Content-Type: application/json

{
  "question": "What does this document say about X?",
  "maxResults": 3,
  "similarityThreshold": 0.7,
  "includeSourceInfo": true
}
```

#### Health Check
```http
GET /api/chat/health
```

## Testing with cURL

### 1. Upload a document
```bash
curl -X POST http://localhost:9090/api/documents/upload \\
  -F "file=@example.md"
```

### 2. Check document status
```bash
curl -X GET http://localhost:9090/api/documents
```

### 3. Ask a question
```bash
curl -X POST http://localhost:9090/api/chat/ask \\
  -H "Content-Type: application/json" \\
  -d '{
    "question": "What is this document about?",
    "maxResults": 5,
    "similarityThreshold": 0.7,
    "includeSourceInfo": true
  }'
```

## Configuration

### Environment Variables
- `ZTOKEN`: Zalando ZLLM API token (provides access to AWS Bedrock models)
- `DB_USERNAME`: Database username (optional, defaults handled by docker-compose)
- `DB_PASSWORD`: Database password (optional, defaults handled by docker-compose)
- `SPRING_PROFILES_ACTIVE`: Active profile (dev, prod)

### Application Properties

#### Legacy Configuration (maintained for compatibility)
- `zeos.rag.chunk-size`: Text chunk size (default: 1000)
- `zeos.rag.chunk-overlap`: Overlap between chunks (default: 200)
- `zeos.rag.max-results`: Maximum search results (default: 5)
- `zeos.rag.similarity-threshold`: Minimum similarity score (default: 0.3)
- `zeos.rag.max-file-size`: Maximum file upload size (default: 10MB)
- `zeos.rag.allowed-file-types`: Allowed file extensions (default: md,markdown,txt)

#### Advanced Chunking Configuration
- `rag.chunking.default-strategy`: Default chunking strategy (default: intelligent)
- `rag.chunking.global.min-chunk-size`: Minimum chunk size (default: 200)
- `rag.chunking.global.max-chunk-size`: Maximum chunk size (default: 2000)
- `rag.chunking.global.preferred-chunk-size`: Preferred chunk size (default: 800)
- `rag.chunking.global.overlap-size`: Overlap between chunks (default: 100)
- `rag.chunking.global.preserve-code-blocks`: Preserve code blocks (default: true)
- `rag.chunking.global.maintain-sentence-boundaries`: Maintain sentence boundaries (default: true)

## Error Responses

All endpoints return structured error responses:

```json
{
  "error": "Error type",
  "message": "Detailed error message",
  "status": 400
}
```

Common HTTP status codes:
- `200`: Success
- `400`: Bad Request (validation errors, unsupported file types)
- `404`: Not Found (document not found)
- `500`: Internal Server Error

## Learning and Experimentation

This API is designed in a way that it can be used for hands-on learning of RAG concepts:

- **Document Analysis**: Upload different types of documents to see how the system automatically selects chunking strategies
- **Vector Search**: Experiment with different similarity thresholds and result counts to understand retrieval behavior
- **Chunking Strategies**: Observe how different document types are processed and chunked in the database
- **Spring AI Patterns**: Explore the integration patterns between Spring AI, vector stores, and language models

**Database Exploration**: Use pgAdmin (http://localhost:5050) to examine:
- Vector embeddings in the `vector_store` table
- Document metadata and chunking results
- HNSW index performance and structure
