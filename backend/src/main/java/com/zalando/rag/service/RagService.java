package com.zalando.rag.service;

import com.zalando.rag.config.RagProperties;
import com.zalando.rag.dto.ChatRequest;
import com.zalando.rag.dto.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
  private final ChatMemory chatMemory;
  private final ConversationMemoryService conversationMemoryService;
  private final RagProperties ragProperties;

  private static final String RAG_SYSTEM_PROMPT =
      """
            You are a helpful AI assistant that answers questions based on the provided context from documents.
            Use the following context to answer questions. If the answer is not in the context,
            say "I don't have enough information to answer this question based on the provided documents."

            You can refer to previous parts of our conversation to provide better, more coherent answers.
            """;

  private static final String RAG_CONTEXT_TEMPLATE =
      """
            Here is the relevant context from the documents for your current question:

            {context}
            """;

  public ChatResponse askQuestion(ChatRequest request) {
    long startTime = System.currentTimeMillis();

    log.debug("Processing question: {}", request.getQuestion());

    try {
      // Determine conversation ID (use conversationId if provided, otherwise sessionId)
      String conversationId =
          request.getConversationId() != null
              ? request.getConversationId()
              : request.getSessionId();

      if (conversationId == null) {
        conversationId = "default-conversation";
        log.warn("No conversation ID or session ID provided, using default");
      }

      // Search for relevant documents
      List<Document> relevantDocs =
          vectorStoreService.searchSimilar(
              request.getQuestion(), request.getMaxResults(), request.getSimilarityThreshold());

      String answer;
      if (relevantDocs.isEmpty()) {
        log.info("No relevant documents found for question: {}", request.getQuestion());

        var existingMessages = conversationMemoryService.getOptimizedMessages(conversationId);
        boolean hasConversationHistory = !existingMessages.isEmpty();

        if (!hasConversationHistory) {
          log.info("First message in conversation without relevant documents");
          answer =
              "I don't have enough information to answer this question based on the available documents. "
                  + "Please make sure relevant documents have been uploaded and processed.";
        } else {
          // Documents were found in earlier turns - answer based on conversation history
          log.info("No new documents found, answering based on conversation history");

          var messages = new ArrayList<>(existingMessages);
          messages.add(new SystemMessage(RAG_SYSTEM_PROMPT));
          messages.add(new UserMessage(request.getQuestion()));

          answer = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();

          if (answer == null || answer.trim().isEmpty()) {
            answer =
                "I'm sorry, I couldn't generate a response. Please try rephrasing your question.";
          }
        }

        // Store the conversation using optimized memory service
        conversationMemoryService.addMessage(
            conversationId, new UserMessage(request.getQuestion()));
        conversationMemoryService.addMessage(conversationId, new AssistantMessage(answer));
      } else {
        // Build context from relevant documents
        String context = buildContext(relevantDocs);

        // Create context message with template
        PromptTemplate contextTemplate = new PromptTemplate(RAG_CONTEXT_TEMPLATE);
        String contextMessage = contextTemplate.render(Map.of("context", context));

        // Get optimized conversation history with context
        var messages =
            new ArrayList<>(conversationMemoryService.getOptimizedMessages(conversationId));
        messages.add(new SystemMessage(RAG_SYSTEM_PROMPT));
        messages.add(
            new UserMessage(contextMessage + "\n\nUser question: " + request.getQuestion()));

        // Use chat model with memory, providing context and user question
        answer = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();

        // Store the conversation using optimized memory service
        conversationMemoryService.addMessage(
            conversationId, new UserMessage(request.getQuestion()));
        conversationMemoryService.addMessage(conversationId, new AssistantMessage(answer));
      }

      // Build response with sources
      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long responseTime = System.currentTimeMillis() - startTime;

      log.info(
          "Successfully answered question in {}ms with {} sources for conversation: {}",
          responseTime,
          sources.size(),
          conversationId);

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

    log.debug("Processing question within document {}: {}", documentId, request.getQuestion());

    try {
      // Determine conversation ID (use conversationId if provided, otherwise sessionId)
      String conversationId =
          request.getConversationId() != null
              ? request.getConversationId()
              : request.getSessionId();

      if (conversationId == null) {
        conversationId = "doc-" + documentId + "-conversation";
        log.warn("No conversation ID or session ID provided for document query, using default");
      }

      // Search for relevant documents within specific document
      List<Document> relevantDocs =
          vectorStoreService.searchSimilarByDocument(
              request.getQuestion(),
              documentId,
              request.getMaxResults(),
              request.getSimilarityThreshold());

      String documentSpecificSystemPrompt =
          """
                    You are a helpful AI assistant answering questions about a specific document.
                    Use the following context from the document to answer questions.
                    If the answer is not in the provided context, say "I don't have enough information
                    to answer this question based on the content of this document."

                    You can refer to previous parts of our conversation about this document to provide better answers.
                    """;

      String answer;
      if (relevantDocs.isEmpty()) {
        log.debug(
            "No relevant content found in document {} for question: {}",
            documentId,
            request.getQuestion());

        var existingMessages = conversationMemoryService.getOptimizedMessages(conversationId);
        boolean hasConversationHistory = !existingMessages.isEmpty();

        if (!hasConversationHistory) {
          log.info("First message about document {} without relevant content", documentId);
          answer =
              "I don't have enough information to answer this question based on the content of this document.";
        } else {
          // Documents were found in earlier turns - answer based on conversation history
          log.info(
              "No new content found in document {}, answering based on conversation history",
              documentId);

          var messages = new ArrayList<>(existingMessages);
          messages.add(new SystemMessage(documentSpecificSystemPrompt));
          messages.add(new UserMessage(request.getQuestion()));

          answer = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();

          if (answer == null || answer.trim().isEmpty()) {
            answer =
                "I'm sorry, I couldn't generate a response. Please try rephrasing your question.";
          }
        }

        // Store the conversation using optimized memory service
        conversationMemoryService.addMessage(
            conversationId, new UserMessage(request.getQuestion()));
        conversationMemoryService.addMessage(conversationId, new AssistantMessage(answer));
      } else {
        // Build context from relevant documents
        String context = buildContext(relevantDocs);

        // Create context message with template
        PromptTemplate contextTemplate = new PromptTemplate(RAG_CONTEXT_TEMPLATE);
        String contextMessage = contextTemplate.render(Map.of("context", context));

        var messages =
            new ArrayList<>(conversationMemoryService.getOptimizedMessages(conversationId));
        messages.add(new SystemMessage(documentSpecificSystemPrompt));
        messages.add(
            new UserMessage(contextMessage + "\n\nUser question: " + request.getQuestion()));

        answer = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();

        // Store the conversation using optimized memory service
        conversationMemoryService.addMessage(
            conversationId, new UserMessage(request.getQuestion()));
        conversationMemoryService.addMessage(conversationId, new AssistantMessage(answer));
      }

      // Build response with sources
      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long responseTime = System.currentTimeMillis() - startTime;

      log.info(
          "Successfully answered question within document {} in {}ms with {} sources for conversation: {}",
          documentId,
          responseTime,
          sources.size(),
          conversationId);

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

  // TODO: Work in progress - This method will be used later for similarity score implementation
  /*private ChatResponse.SourceDocument mapToSourceDocumentWithScore(VectorStoreService.DocumentWithScore docWithScore) {
    Document doc = docWithScore.getDocument();
    return ChatResponse.SourceDocument.builder()
        .filename(doc.getMetadata().getOrDefault("filename", "Unknown").toString())
        .title(doc.getMetadata().getOrDefault("title", "").toString())
        .content(truncateContent(doc.getText(), 500))
        .similarity(docWithScore.getSimilarityScore())
        .chunkIndex(Integer.parseInt(doc.getMetadata().getOrDefault("chunk_index", "0").toString()))
        .build();
  }
  */

  private ChatResponse.SourceDocument mapToSourceDocument(Document doc) {
    return ChatResponse.SourceDocument.builder()
        .filename(doc.getMetadata().getOrDefault("filename", "Unknown").toString())
        .title(doc.getMetadata().getOrDefault("title", "").toString())
        .content(truncateContent(doc.getText(), 500)) // Truncate for response size
        .similarity(0.0) // TODO: Work in progress - similarity scores will be implemented later
        .chunkIndex(Integer.parseInt(doc.getMetadata().getOrDefault("chunk_index", "0").toString()))
        .build();
  }

  private String truncateContent(String content, int maxLength) {
    if (content.length() <= maxLength) {
      return content;
    }
    return content.substring(0, maxLength) + "...";
  }
}
