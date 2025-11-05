-- Initialize the database with pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create user for the application
CREATE USER zeos_user WITH PASSWORD 'zeos_password';
GRANT ALL PRIVILEGES ON DATABASE zeos_rag_dev TO zeos_user;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO zeos_user;