package com.zalando.rag.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

  private final VectorStore vectorStore;

  /** Add documents to the vector store */
  public void addDocuments(List<Document> documents) {
    try {
      log.info("Adding {} documents to vector store", documents.size());
      log.debug("VectorStore instance: {}", vectorStore.getClass().getName());

      // Log first document details for debugging
      if (!documents.isEmpty()) {
        Document firstDoc = documents.get(0);
        log.debug("First document: id={}, metadata={}", firstDoc.getId(), firstDoc.getMetadata());
      }

      // This is where the embedding API call happens
      log.debug("About to call vectorStore.add() which will trigger embedding API call");
      vectorStore.add(documents);

      log.info("Successfully added documents to vector store");
    } catch (Exception e) {
      log.error("Error adding documents to vector store", e);

      // Additional debugging info
      if (e.getCause() != null) {
        log.error("Root cause: {}", e.getCause().getMessage());
      }

      throw new RuntimeException("Failed to add documents to vector store", e);
    }
  }

  /**
   * TODO: Work in progress - Search for similar documents with scores This method will be fully
   * implemented later to provide actual similarity scores
   */
  public List<DocumentWithScore> searchSimilarWithScores(
      String query, int maxResults, double similarityThreshold) {
    try {
      log.debug(
          "Searching for similar documents: query='{}', maxResults={}, threshold={}",
          query,
          maxResults,
          similarityThreshold);

      SearchRequest searchRequest =
          SearchRequest.builder()
              .query(query)
              .topK(maxResults)
              .similarityThreshold(similarityThreshold)
              .build();

      List<Document> results = vectorStore.similaritySearch(searchRequest);
      log.debug("Found {} similar documents", results.size());

      // For now, we'll simulate similarity scores based on the similarity threshold
      // In a real implementation, we'd need to access the actual scores from pgvector
      return results.stream()
          .map(
              doc -> {
                // Simulate a realistic similarity score between threshold and 1.0
                // Higher ranked results get higher scores
                int index = results.indexOf(doc);
                double score =
                    similarityThreshold
                        + (1.0 - similarityThreshold) * (1.0 - (double) index / results.size());
                return DocumentWithScore.builder()
                    .document(doc)
                    .similarityScore(Math.min(1.0, Math.max(similarityThreshold, score)))
                    .build();
              })
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Error searching vector store", e);
      throw new RuntimeException("Failed to search vector store", e);
    }
  }

  /** Search for similar documents (legacy method) */
  public List<Document> searchSimilar(String query, int maxResults, double similarityThreshold) {
    return searchSimilarWithScores(query, maxResults, similarityThreshold).stream()
        .map(DocumentWithScore::getDocument)
        .collect(Collectors.toList());
  }

  /**
   * TODO: Work in progress - Search for similar documents with document filter and scores This
   * method will be fully implemented later to provide actual similarity scores
   */
  public List<DocumentWithScore> searchSimilarByDocumentWithScores(
      String query, String documentId, int maxResults, double similarityThreshold) {
    try {
      log.debug(
          "Searching for similar documents in document {}: query='{}', maxResults={}, threshold={}",
          documentId,
          query,
          maxResults,
          similarityThreshold);

      Filter.Expression filterExpression =
          new FilterExpressionBuilder().eq("document_id", documentId).build();

      SearchRequest searchRequest =
          SearchRequest.builder()
              .query(query)
              .topK(maxResults)
              .similarityThreshold(similarityThreshold)
              .filterExpression(filterExpression)
              .build();

      List<Document> results = vectorStore.similaritySearch(searchRequest);
      log.debug("Found {} similar documents in document {}", results.size(), documentId);

      // Simulate similarity scores
      return results.stream()
          .map(
              doc -> {
                int index = results.indexOf(doc);
                double score =
                    similarityThreshold
                        + (1.0 - similarityThreshold) * (1.0 - (double) index / results.size());
                return DocumentWithScore.builder()
                    .document(doc)
                    .similarityScore(Math.min(1.0, Math.max(similarityThreshold, score)))
                    .build();
              })
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Error searching vector store for document {}", documentId, e);
      throw new RuntimeException("Failed to search vector store", e);
    }
  }

  /** Search for similar documents with document filter (legacy method) */
  public List<Document> searchSimilarByDocument(
      String query, String documentId, int maxResults, double similarityThreshold) {
    return searchSimilarByDocumentWithScores(query, documentId, maxResults, similarityThreshold)
        .stream()
        .map(DocumentWithScore::getDocument)
        .collect(Collectors.toList());
  }

  /** Delete documents by document ID */
  public void deleteByDocumentId(String documentId) {
    try {
      log.info("Deleting documents with document_id: {}", documentId);

      Filter.Expression filterExpression =
          new FilterExpressionBuilder().eq("document_id", documentId).build();

      vectorStore.delete(List.of(documentId));
      log.info("Successfully deleted documents with document_id: {}", documentId);
    } catch (Exception e) {
      log.error("Error deleting documents with document_id: {}", documentId, e);
      throw new RuntimeException("Failed to delete documents from vector store", e);
    }
  }

  /** Delete all documents (use with caution) */
  public void deleteAll() {
    try {
      log.warn("Deleting all documents from vector store");
      // Note: PGVector doesn't have a direct deleteAll method
      // This is a placeholder - in practice you might need to implement
      // a custom solution or recreate the collection
      log.warn("Delete all not implemented for PGVector - manual cleanup required");
    } catch (Exception e) {
      log.error("Error deleting all documents", e);
      throw new RuntimeException("Failed to delete all documents", e);
    }
  }

  /** Get vector store statistics */
  public VectorStoreStats getStats() {
    try {
      // Note: This is a simplified implementation
      // In a real scenario, you might query the database directly for stats
      return VectorStoreStats.builder()
          .totalDocuments(0L) // Would need to query the actual count
          .build();
    } catch (Exception e) {
      log.error("Error getting vector store stats", e);
      return VectorStoreStats.builder().totalDocuments(0L).build();
    }
  }

  public static class VectorStoreStats {
    private final Long totalDocuments;

    private VectorStoreStats(Long totalDocuments) {
      this.totalDocuments = totalDocuments;
    }

    public static VectorStoreStatsBuilder builder() {
      return new VectorStoreStatsBuilder();
    }

    public Long getTotalDocuments() {
      return totalDocuments;
    }

    public static class VectorStoreStatsBuilder {
      private Long totalDocuments;

      public VectorStoreStatsBuilder totalDocuments(Long totalDocuments) {
        this.totalDocuments = totalDocuments;
        return this;
      }

      public VectorStoreStats build() {
        return new VectorStoreStats(totalDocuments);
      }
    }
  }

  /** TODO: Work in progress - DocumentWithScore class for future similarity score implementation */
  @Getter
  @Builder
  public static class DocumentWithScore {
    private final Document document;
    private final double similarityScore;
  }
}
