package com.zalando.rag.service;

import com.zalando.presto.tools.PrestoQueryTools;
import com.zalando.rag.config.RagProperties;
import com.zalando.rag.dto.ChatRequest;
import com.zalando.rag.dto.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

  private final VectorStoreService vectorStoreService;
  private final ChatModel chatModel;
  private final ConversationMemoryService conversationMemoryService;
  private final RagProperties ragProperties;
  private final DatalakeMetadataService datalakeMetadataService;

  // Injected only when presto.enabled=true (optional dependency)
  @Autowired(required = false)
  private PrestoQueryTools prestoQueryTools;

  // ---------------------------------------------------------------------------
  // System prompts
  // ---------------------------------------------------------------------------

  private static final String BASE_SYSTEM_PROMPT =
      """
      You are a helpful AI assistant for the Zalando ZEOS platform.

      You have two sources of information:
      1. **Document knowledge base** – retrieved context chunks are included in the user message.
      2. **Datalake tools** – presto_* tools let you query live data from Trino/Presto.

      Decision rules:
      - If the question can be answered from the provided document context, answer directly.
      - If the question requires live data, aggregations, or metrics (counts, totals, trends),
        use the presto_* tools. Always call presto_describe_table before writing a SELECT query
        against a table you haven't seen in this conversation.
      - If both sources are relevant, combine them for a richer answer.
      - If you cannot answer from either source, say so clearly.

      Be concise and factual. Cite the source (document or table name) when relevant.
      """;

  private static final String DATALAKE_CONTEXT_SECTION =
      """

      ---
      {datalakeMetadata}
      ---
      """;

  private static final String RAG_HISTORY_ONLY_PROMPT =
      """
      Answer the question strictly based on the conversation history above.
      Do NOT use general knowledge outside what has been discussed.
      If the answer is not in the history, say so clearly.
      """;

  private static final String DOC_CONTEXT_PREFIX =
      """
      ## Relevant document context

      {context}

      ---
      """;

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  public ChatResponse askQuestion(ChatRequest request) {
    long start = System.currentTimeMillis();
    try {
      String conversationId = resolveConversationId(request);

      List<Document> relevantDocs =
          vectorStoreService.searchSimilar(
              request.getQuestion(), request.getMaxResults(), request.getSimilarityThreshold());

      String answer = generateAnswer(request.getQuestion(), relevantDocs, conversationId);

      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long elapsed = System.currentTimeMillis() - start;
      log.info(
          "Answered in {}ms, {} RAG sources, tools={} for conversation {}",
          elapsed,
          sources.size(),
          prestoQueryTools != null,
          conversationId);

      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(answer)
          .sources(request.isIncludeSourceInfo() ? sources : List.of())
          .responseTimeMs(elapsed)
          .build();

    } catch (Exception e) {
      log.error("Error processing question: {}", request.getQuestion(), e);
      return errorResponse(request.getQuestion(), System.currentTimeMillis() - start);
    }
  }

  public ChatResponse askQuestionWithinDocument(String documentId, ChatRequest request) {
    long start = System.currentTimeMillis();
    try {
      String conversationId = resolveConversationId(request);

      List<Document> relevantDocs =
          vectorStoreService.searchSimilarByDocument(
              request.getQuestion(),
              documentId,
              request.getMaxResults(),
              request.getSimilarityThreshold());

      String docSystemPrompt =
          "You are answering questions about a specific document. "
              + "Use only the provided document context. "
              + "If the answer is not in the context, say so.";

      String answer =
          generateAnswerWithSystemPrompt(
              request.getQuestion(), relevantDocs, conversationId, docSystemPrompt);

      List<ChatResponse.SourceDocument> sources =
          relevantDocs.stream().map(this::mapToSourceDocument).collect(Collectors.toList());

      long elapsed = System.currentTimeMillis() - start;
      return ChatResponse.builder()
          .question(request.getQuestion())
          .answer(answer)
          .sources(request.isIncludeSourceInfo() ? sources : List.of())
          .responseTimeMs(elapsed)
          .build();

    } catch (Exception e) {
      log.error("Error processing doc question for {}: {}", documentId, request.getQuestion(), e);
      return errorResponse(request.getQuestion(), System.currentTimeMillis() - start);
    }
  }

  // ---------------------------------------------------------------------------
  // Core generation
  // ---------------------------------------------------------------------------

  private String generateAnswer(
      String question, List<Document> relevantDocs, String conversationId) {
    return generateAnswerWithSystemPrompt(
        question, relevantDocs, conversationId, buildSystemPrompt());
  }

  private String generateAnswerWithSystemPrompt(
      String question, List<Document> relevantDocs, String conversationId, String systemPrompt) {

    List<Message> history =
        new ArrayList<>(conversationMemoryService.getOptimizedMessages(conversationId));

    // If no docs and no history, return early without burning tokens
    if (relevantDocs.isEmpty() && history.isEmpty()) {
      return "I don't have enough information to answer this question based on the available documents. "
          + "Please make sure relevant documents have been uploaded and processed.";
    }

    // If no docs but we have history, answer from history only
    if (relevantDocs.isEmpty()) {
      String answer = callModel(question, history, systemPrompt, List.of(), null);
      storeExchange(conversationId, question, answer);
      return answer;
    }

    // We have docs — build context and call with optional tools
    String docContext = buildContext(relevantDocs);
    String userMessage = buildUserMessage(question, docContext);

    String answer =
        callModel(
            userMessage,
            history,
            systemPrompt,
            prestoQueryTools != null ? List.of(prestoQueryTools) : List.of(),
            null);
    storeExchange(conversationId, question, answer);
    return answer;
  }

  private String callModel(
      String userMessage,
      List<Message> history,
      String systemPrompt,
      List<Object> tools,
      String overrideHistory) {

    ChatClient client = ChatClient.builder(chatModel).build();

    ChatClient.ChatClientRequestSpec spec =
        client.prompt().system(systemPrompt).messages(history).user(userMessage);

    if (!tools.isEmpty()) {
      spec = spec.tools(tools.toArray());
    }

    String result = spec.call().content();
    return result != null && !result.isBlank()
        ? result
        : "I'm sorry, I couldn't generate a response. Please try rephrasing your question.";
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private String buildSystemPrompt() {
    if (datalakeMetadataService.hasMetadata() && prestoQueryTools != null) {
      return BASE_SYSTEM_PROMPT
          + DATALAKE_CONTEXT_SECTION.replace(
              "{datalakeMetadata}", datalakeMetadataService.getMetadataPromptFragment());
    }
    return BASE_SYSTEM_PROMPT;
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

  private String buildUserMessage(String question, String context) {
    return DOC_CONTEXT_PREFIX.replace("{context}", context) + "\n\n" + question;
  }

  private void storeExchange(String conversationId, String question, String answer) {
    conversationMemoryService.addMessage(conversationId, new UserMessage(question));
    conversationMemoryService.addMessage(conversationId, new AssistantMessage(answer));
  }

  private String resolveConversationId(ChatRequest request) {
    if (request.getConversationId() != null) return request.getConversationId();
    if (request.getSessionId() != null) return request.getSessionId();
    log.warn("No conversation ID provided, using default");
    return "default-conversation";
  }

  private ChatResponse errorResponse(String question, long elapsed) {
    return ChatResponse.builder()
        .question(question)
        .answer(
            "I'm sorry, but I encountered an error while processing your question. Please try again later.")
        .sources(List.of())
        .responseTimeMs(elapsed)
        .build();
  }

  private ChatResponse.SourceDocument mapToSourceDocument(Document doc) {
    return ChatResponse.SourceDocument.builder()
        .filename(doc.getMetadata().getOrDefault("filename", "Unknown").toString())
        .title(doc.getMetadata().getOrDefault("title", "").toString())
        .content(truncate(doc.getText(), 500))
        .similarity(0.0)
        .chunkIndex(Integer.parseInt(doc.getMetadata().getOrDefault("chunk_index", "0").toString()))
        .build();
  }

  private String truncate(String content, int max) {
    return content.length() <= max ? content : content.substring(0, max) + "...";
  }
}
