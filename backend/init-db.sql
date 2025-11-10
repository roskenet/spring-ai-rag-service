-- This script runs when PostgreSQL container starts with fresh data
-- The database 'spring_ai_rag_dev' is already created by POSTGRES_DB env var

-- Install required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create application user (if not exists)
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'zeos_user') THEN

      CREATE ROLE zeos_user LOGIN PASSWORD 'zeos_password';
   END IF;
END
$do$;

-- Grant database privileges to the user
GRANT ALL PRIVILEGES ON DATABASE spring_ai_rag_dev TO zeos_user;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO zeos_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO zeos_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO zeos_user;

-- Grant default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zeos_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zeos_user;