#!/bin/bash
set -e

# Create network
podman network create zeos-rag-network 2>/dev/null || true

# Start PostgreSQL
echo "Starting PostgreSQL..."
podman run -d \
  --name zeos-rag-postgres \
  --network zeos-rag-network \
  -e POSTGRES_DB=spring_ai_rag_dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  pgvector/pgvector:pg16 \
  postgres -c shared_preload_libraries=vector

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 10

# Start Backend
echo "Starting Backend..."
podman run -d \
  --name zeos-rag-backend \
  --network zeos-rag-network \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://zeos-rag-postgres:5432/spring_ai_rag_dev \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -v ~/.aws/credentials:/root/.aws/credentials:ro \
  -p 8080:8080 \
  localhost/spring-ai-rag-service_backend:latest

# Wait for Backend to be ready
echo "Waiting for Backend to be ready..."
sleep 20

# Start Frontend
echo "Starting Frontend..."
podman run -d \
  --name zeos-rag-frontend \
  --network zeos-rag-network \
  -e NODE_ENV=production \
  -e BACKEND_API_URL=http://zeos-rag-backend:8080 \
  -p 3000:3000 \
  localhost/spring-ai-rag-service_frontend:latest

echo ""
echo "✓ Services started!"
echo "Frontend: http://localhost:3000"
echo "Backend API: http://localhost:8080"
echo "Database: localhost:5432"
