package com.zalando.rag.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("dev")
public class MockVectorStoreService extends VectorStoreService {

  public MockVectorStoreService() {
    super(null); // No actual vector store needed for mock
  }

  @Override
  public void addDocuments(List<Document> documents) {
    log.warn("MOCK MODE: Simulating adding {} documents to vector store", documents.size());
    log.info("Documents would contain {} chunks", documents.size());

    // In mock mode, we just log the documents instead of storing them
    for (int i = 0; i < documents.size(); i++) {
      Document doc = documents.get(i);
      log.debug("Mock storing document chunk {}: {} characters", i, doc.getText().length());
    }

    log.info("Mock vector store addition completed successfully");
  }

  @Override
  public List<Document> searchSimilar(String query, int maxResults, double similarityThreshold) {
    log.warn("MOCK MODE: Simulating similarity search for query: '{}'", query);

    // Return mock documents that would be similar to the query
    Document mockDoc1 =
        new Document(
            "This is a mock document about "
                + query
                + ". "
                + "Spring Boot RAG applications combine retrieval and generation techniques "
                + "to provide accurate answers based on your documents.",
            Map.of(
                "filename", "mock-document.md",
                "title", "Mock Document",
                "chunk_index", "0",
                "document_id", "1"));

    Document mockDoc2 =
        new Document(
            "Another mock result for "
                + query
                + ". "
                + "RAG systems use vector databases to find relevant information "
                + "and then generate contextual responses using language models.",
            Map.of(
                "filename", "mock-document.md",
                "title", "Mock Document",
                "chunk_index", "1",
                "document_id", "1"));

    List<Document> mockResults = List.of(mockDoc1, mockDoc2);
    log.info("Mock search returning {} results", mockResults.size());

    return mockResults;
  }

  @Override
  public List<Document> searchSimilarByDocument(
      String query, String documentId, int maxResults, double similarityThreshold) {
    log.warn(
        "MOCK MODE: Simulating document-specific search for query: '{}' in document: {}",
        query,
        documentId);
    return searchSimilar(query, maxResults, similarityThreshold);
  }

  @Override
  public void deleteByDocumentId(String documentId) {
    log.warn("MOCK MODE: Simulating deletion of documents with ID: {}", documentId);
  }
}
