-- Fix vector_store embedding dimension from 1536 to 1024
-- Required because amazon.titan-embed-text-v2:0 produces 1024-dimensional vectors by default.
-- All existing vectors are invalid (produced with wrong model), so we truncate and recreate.

DROP INDEX IF EXISTS spring_ai_vector_index;

-- Clear existing data (invalid vectors from previous model)
TRUNCATE TABLE "public".vector_store;

-- Change column type to 1024 dimensions
ALTER TABLE "public".vector_store
    ALTER COLUMN embedding TYPE vector(1024);

-- Recreate HNSW index
CREATE INDEX spring_ai_vector_index ON "public".vector_store USING hnsw ( embedding vector_cosine_ops );
