# Spring Boot RAG Application Guide

## Introduction

This document provides a comprehensive guide to building and deploying Retrieval-Augmented Generation (RAG) applications using Spring Boot and Spring AI.

## What is RAG?

Retrieval-Augmented Generation (RAG) is a powerful AI technique that combines:

1. **Information Retrieval**: Finding relevant documents from a knowledge base
2. **Text Generation**: Using large language models to generate contextual responses
3. **Vector Embeddings**: Converting text into numerical representations for similarity search

### Benefits of RAG

- **Accuracy**: Provides factual information from your own documents
- **Transparency**: Shows sources used for generating answers
- **Updatable**: Easy to add new information without retraining models
- **Cost-effective**: Uses pre-trained models with your data

## Architecture Components

### Document Processing Pipeline

1. **Document Ingestion**: Upload markdown, text, or other document formats
2. **Text Chunking**: Split documents into manageable pieces
3. **Vectorization**: Convert text chunks into embeddings
4. **Storage**: Store vectors in a vector database (PGVector)

### Query Processing

1. **Question Analysis**: Process user questions
2. **Vector Search**: Find relevant document chunks
3. **Context Building**: Combine relevant chunks
4. **Answer Generation**: Use LLM to generate contextual responses

## Implementation Details

### Technologies Used

- **Spring Boot 3.5**: Modern Java framework
- **Spring AI**: AI integration framework
- **PostgreSQL + PGVector**: Vector database
- **OpenAI API**: Language model and embeddings
- **Docker**: Containerization

### Key Features

- RESTful API endpoints
- Asynchronous document processing
- Configurable chunk sizes and similarity thresholds
- Source attribution in responses
- Error handling and validation

## Getting Started

### Prerequisites

1. Java 21 or higher
2. PostgreSQL with PGVector extension
3. AWS account with Bedrock access

### Quick Setup

1. Clone the repository
2. Configure AWS credentials and set environment variables (AWS_REGION, DB_USERNAME, DB_PASSWORD)
3. Start PostgreSQL with Docker: `docker-compose up -d postgres`
4. Run the application: `./gradlew bootRun --args='--spring.profiles.active=dev'`

### Upload Your First Document

Use the REST API to upload a markdown file:

```bash
curl -X POST http://localhost:8080/api/documents/upload \\
  -F "file=@your-document.md"
```

### Ask Questions

Query your documents:

```bash
curl -X POST http://localhost:8080/api/chat/ask \\
  -H "Content-Type: application/json" \\
  -d '{
    "question": "What is RAG and how does it work?",
    "maxResults": 5
  }'
```

## Advanced Configuration

### Tuning Parameters

- **Chunk Size**: Larger chunks provide more context but may reduce precision
- **Chunk Overlap**: Prevents information loss at chunk boundaries
- **Similarity Threshold**: Controls relevance filtering
- **Max Results**: Limits the number of sources considered

### Monitoring and Logging

The application provides detailed logging for:
- Document processing status
- Query performance metrics
- Error tracking
- Vector store operations

## Best Practices

### Document Preparation

1. Use clear headings and structure
2. Keep paragraphs focused on single topics
3. Include relevant keywords
4. Maintain consistent formatting

### Query Optimization

1. Ask specific, focused questions
2. Use keywords from your documents
3. Experiment with similarity thresholds
4. Review source attributions

## Troubleshooting

### Common Issues

1. **Document Processing Fails**: Check file format and size limits
2. **No Relevant Results**: Adjust similarity threshold or chunk size
3. **Slow Responses**: Reduce max results or optimize database indexes
4. **API Errors**: Verify environment variables and API keys

## Conclusion

This RAG application demonstrates how to build intelligent document search and question-answering systems using modern Java frameworks. The combination of Spring Boot, Spring AI, and vector databases provides a robust foundation for enterprise AI applications.
