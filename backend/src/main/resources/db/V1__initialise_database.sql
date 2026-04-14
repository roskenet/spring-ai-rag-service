CREATE SCHEMA IF NOT EXISTS "public";

-- Enable required extensions for vector operations
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS "public".document_metrics (
                                            id                   bigint  NOT NULL GENERATED  BY DEFAULT AS IDENTITY ,
                                            access_count         bigint    ,
                                            category             varchar(255)    ,
                                            chunk_count          integer  NOT NULL  ,
                                            chunking_strategy    varchar(255)    ,
                                            created_at           timestamp    ,
                                            document_id          bigint  NOT NULL  ,
                                            document_name        varchar(255)  NOT NULL  ,
                                            document_type        varchar(255)    ,
                                            embedding_model      varchar(255)    ,
                                            file_size            bigint  NOT NULL  ,
                                            last_accessed        timestamp    ,
                                            processing_time_ms   bigint    ,
                                            updated_at           timestamp    ,
                                            CONSTRAINT document_metrics_pkey PRIMARY KEY ( id )
);

CREATE  TABLE IF NOT EXISTS "public".documents (
                                     chunk_count          integer    ,
                                     created_at           timestamp    ,
                                     file_size            bigint    ,
                                     id                   bigint  NOT NULL GENERATED  BY DEFAULT AS IDENTITY ,
                                     updated_at           timestamp    ,
                                     content              text    ,
                                     content_hash         varchar(255)    ,
                                     error_message        varchar(255)    ,
                                     filename             varchar(255)  NOT NULL  ,
                                     status               varchar(255)  NOT NULL  ,
                                     title                varchar(255)  NOT NULL  ,
                                     CONSTRAINT documents_pkey PRIMARY KEY ( id ),
                                     CONSTRAINT documents_content_hash_key UNIQUE ( content_hash )
);

CREATE  TABLE IF NOT EXISTS "public".query_metrics (
                                         id                   bigint  NOT NULL GENERATED  BY DEFAULT AS IDENTITY ,
                                         accuracy_score       double precision    ,
                                         created_at           timestamp    ,
                                         error_message        varchar(255)    ,
                                         max_results          integer    ,
                                         query_text           text  NOT NULL  ,
                                         response_time_ms     bigint  NOT NULL  ,
                                         results_found        integer    ,
                                         selected_model       varchar(255)    ,
                                         similarity_threshold double precision    ,
                                         success              boolean  NOT NULL  ,
                                         temperature          double precision    ,
                                         user_session_id      varchar(255)    ,
                                         CONSTRAINT query_metrics_pkey PRIMARY KEY ( id )
);

CREATE  TABLE IF NOT EXISTS "public".rag_configurations (
                                              id                   bigint  NOT NULL GENERATED  BY DEFAULT AS IDENTITY ,
                                              chunk_size           integer    ,
                                              chunking_strategy    varchar(255)    ,
                                              config_key           varchar(255)  NOT NULL  ,
                                              created_at           timestamp    ,
                                              embeddings_model     varchar(255)    ,
                                              include_citations    boolean    ,
                                              is_active            boolean    ,
                                              max_results          integer    ,
                                              overlap_percentage   integer    ,
                                              selected_model       varchar(255)    ,
                                              similarity_threshold double precision    ,
                                              temperature          double precision    ,
                                              top_k                integer    ,
                                              updated_at           timestamp    ,
                                              CONSTRAINT rag_configurations_pkey PRIMARY KEY ( id ),
                                              CONSTRAINT ukfpr72xoa70ooodttjn5qmbwvg UNIQUE ( config_key )
);

CREATE  TABLE IF NOT EXISTS "public".system_metrics (
                                          id                   bigint  NOT NULL GENERATED  BY DEFAULT AS IDENTITY ,
                                          metadata             text    ,
                                          metric_type          varchar(255)  NOT NULL  ,
                                          metric_unit          varchar(255)    ,
                                          metric_value         double precision  NOT NULL  ,
                                          recorded_at          timestamp    ,
                                          CONSTRAINT system_metrics_pkey PRIMARY KEY ( id )
);

CREATE  TABLE IF NOT EXISTS "public".vector_store (
                                        id                   uuid DEFAULT uuid_generate_v4() NOT NULL  ,
                                        content              text    ,
                                        metadata             json    ,
                                        embedding            vector(1024)    ,
                                        CONSTRAINT vector_store_pkey PRIMARY KEY ( id )
);

CREATE INDEX spring_ai_vector_index ON "public".vector_store USING  hnsw ( embedding  vector_cosine_ops );
