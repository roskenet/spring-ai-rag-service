package com.zalando.rag.repository;

import com.zalando.rag.entity.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

  Optional<Document> findByContentHash(String contentHash);

  List<Document> findByStatus(Document.DocumentStatus status);

  Optional<Document> findByFilename(String filename);

  @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt DESC")
  List<Document> findByStatusOrderByCreatedAtDesc(@Param("status") Document.DocumentStatus status);

  @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'PROCESSED'")
  long countProcessedDocuments();

  @Query("SELECT SUM(d.chunkCount) FROM Document d WHERE d.status = 'PROCESSED'")
  Long getTotalChunkCount();
}
