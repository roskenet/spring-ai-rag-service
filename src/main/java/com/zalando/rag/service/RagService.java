package com.zalando.rag.service;

import com.zalando.rag.config.RagProperties;
import com.zalando.rag.dto.ChatRequest;
import com.zalando.rag.dto.ChatResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

  private final VectorStoreService vectorStoreService;
  private final ChatModel chatModel;
  private final RagProperties ragProperties;

  private static final String RAG_PROMPT_TEMPLATE =
      """
            You are a helpful AI assistant that answers questions based on the provided context.
            Use the following context to answer the question. If the answer is not in the context,
            say "I don't have enough information to answer this question based on the provided documents."

            Context:
            {context}

            Question: {question}

            Answer:
            """;

  public ChatResponse askQuestion(ChatRequest request) {
    long startTime = System.currentTimeMillis();

    log.info("Processing question: {}", request.getQuestion());

    try {
      // Search for relevant documents
      List<Document> relevantDocs =
          vectorStoreService.searchSimilar(
              request.getQuestion(), request.getMaxResults(), request.getSimilarityThreshold());

      if (relevantDocs.isEmpty()) {
        log.info("No relevant documents found for question: {}", request.getQuestion());
        return createNoContextResponse(request, startTime);
      }

      // Build context from relevant documents
      String context = buildContext(relevantDocs);

      // Create prompt
      PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
      Prompt prompt =
          promptTemplate.create(Map.of("context", context, "question", request.getQuestion()));

      // Get answer from AI
      org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);
      String answer = aiResponse.getResult().getOutput().getText();

      // Build response with sources
      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long responseTime = System.currentTimeMillis() - startTime;

      log.info(
          "Successfully answered question in {}ms with {} sources", responseTime, sources.size());

      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(answer)
          .sources(request.isIncludeSourceInfo() ? sources : List.of())
          .responseTimeMs(responseTime)
          .build();

    } catch (Exception e) {
      log.error("Error processing question: {}", request.getQuestion(), e);
      long responseTime = System.currentTimeMillis() - startTime;
      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(
              "I'm sorry, but I encountered an error while processing your question. Please try again later.")
          .sources(List.of())
          .responseTimeMs(responseTime)
          .build();
    }
  }

  public ChatResponse askQuestionWithinDocument(String documentId, ChatRequest request) {
    long startTime = System.currentTimeMillis();

    log.info("Processing question within document {}: {}", documentId, request.getQuestion());

    try {
      // Search for relevant documents within specific document
      List<Document> relevantDocs =
          vectorStoreService.searchSimilarByDocument(
              request.getQuestion(),
              documentId,
              request.getMaxResults(),
              request.getSimilarityThreshold());

      if (relevantDocs.isEmpty()) {
        log.info(
            "No relevant content found in document {} for question: {}",
            documentId,
            request.getQuestion());
        return createNoContextResponse(request, startTime);
      }

      // Build context from relevant documents
      String context = buildContext(relevantDocs);

      // Create prompt with document-specific context
      String documentSpecificPrompt =
          """
                    You are a helpful AI assistant answering questions about a specific document.
                    Use the following context from the document to answer the question.
                    If the answer is not in the provided context, say "I don't have enough information
                    to answer this question based on the content of this document."

                    Context from document:
                    {context}

                    Question: {question}

                    Answer:
                    """;

      PromptTemplate promptTemplate = new PromptTemplate(documentSpecificPrompt);
      Prompt prompt =
          promptTemplate.create(Map.of("context", context, "question", request.getQuestion()));

      // Get answer from AI
      org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);
      String answer = aiResponse.getResult().getOutput().getText();

      // Build response with sources
      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long responseTime = System.currentTimeMillis() - startTime;

      log.info(
          "Successfully answered question within document {} in {}ms with {} sources",
          documentId,
          responseTime,
          sources.size());

      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(answer)
          .sources(request.isIncludeSourceInfo() ? sources : List.of())
          .responseTimeMs(responseTime)
          .build();

    } catch (Exception e) {
      log.error(
          "Error processing question within document {}: {}", documentId, request.getQuestion(), e);
      long responseTime = System.currentTimeMillis() - startTime;
      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(
              "I'm sorry, but I encountered an error while processing your question. Please try again later.")
          .sources(List.of())
          .responseTimeMs(responseTime)
          .build();
    }
  }

  private String buildContext(List<Document> documents) {
    return documents.stream()
        .map(
            doc -> {
              String filename = doc.getMetadata().getOrDefault("filename", "Unknown").toString();
              String chunkIndex = doc.getMetadata().getOrDefault("chunk_index", "0").toString();
              return String.format(
                  "[Source: %s, Chunk: %s]\n%s", filename, chunkIndex, doc.getText());
            })
        .collect(Collectors.joining("\n\n---\n\n"));
  }

  private ChatResponse.SourceDocument mapToSourceDocument(Document doc) {
    return ChatResponse.SourceDocument.builder()
        .filename(doc.getMetadata().getOrDefault("filename", "Unknown").toString())
        .title(doc.getMetadata().getOrDefault("title", "").toString())
        .content(truncateContent(doc.getText(), 500)) // Truncate for response size
        .similarity(0.0) // Would need to extract from search results if available
        .chunkIndex(Integer.parseInt(doc.getMetadata().getOrDefault("chunk_index", "0").toString()))
        .build();
  }

  private String truncateContent(String content, int maxLength) {
    if (content.length() <= maxLength) {
      return content;
    }
    return content.substring(0, maxLength) + "...";
  }

  private ChatResponse createNoContextResponse(ChatRequest request, long startTime) {
    long responseTime = System.currentTimeMillis() - startTime;
    return ChatResponse.builder()
        .question(request.getQuestion())
        .answer(
            "I don't have enough information to answer this question based on the available documents. "
                + "Please make sure relevant documents have been uploaded and processed.")
        .sources(List.of())
        .responseTimeMs(responseTime)
        .build();
  }
}
