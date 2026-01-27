package com.zalando.rag.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryService {

  private final ChatMemory chatMemory;
  private final ChatModel chatModel;

  // Debug counter for tracking message additions
  private static final java.util.Map<String, Integer> messageCountMap =
      new java.util.concurrent.ConcurrentHashMap<>();

  // Configuration constants
  private static final int MAX_MESSAGES_BEFORE_SUMMARIZATION = 15;

  private static final String SUMMARIZATION_PROMPT =
      """
      Please create a concise summary of the following conversation between a user and an AI assistant.
      If there is "Previous Context" included, this represents earlier parts of the conversation that were already summarized.
      You should incorporate that previous context with the new conversation parts to create a comprehensive summary.

      Focus on key information that would be important for continuing the conversation:
      - Important facts the user has shared (from both previous context and new messages)
      - Key topics discussed (from both previous context and new messages)
      - Any decisions or conclusions reached
      - Context that would be needed for future questions

      Keep the summary under 250 words and write it from the perspective of continuing the conversation.

      Conversation to summarize:
      %s

      Summary:
      """;

  /** Gets optimized conversation history with memory management */
  public List<Message> getOptimizedMessages(String conversationId) {
    var allMessages = new ArrayList<>(chatMemory.get(conversationId));

    log.info("=== CONVERSATION MEMORY OPTIMIZATION ===");
    log.info("Conversation ID: {}, Total messages: {}", conversationId, allMessages.size());

    // Check actual size vs expected
    log.info("Raw memory size for {}: {}", conversationId, allMessages.size());

    // Let Spring AI MessageWindowChatMemory handle its own limits
    // We only add summarization for very long conversations
    if (allMessages.size() < MAX_MESSAGES_BEFORE_SUMMARIZATION) {
      log.info(
          "Messages count ({}) below threshold ({}), no optimization needed",
          allMessages.size(),
          MAX_MESSAGES_BEFORE_SUMMARIZATION);
      return allMessages;
    }

    log.info("Messages count ({}) reached threshold, applying summarization", allMessages.size());

    // Estimate token count
    int estimatedTokens = estimateTokenCount(allMessages);
    log.info("Estimated tokens in conversation: {}", estimatedTokens);

    // Apply summarization strategy
    var optimizedMessages = applySummarizationOnly(conversationId, allMessages);

    int optimizedTokens = estimateTokenCount(optimizedMessages);
    log.info(
        "After summarization: {} messages, estimated {} tokens (saved ~{} tokens)",
        optimizedMessages.size(),
        optimizedTokens,
        (estimatedTokens - optimizedTokens));
    log.info("=== END OPTIMIZATION ===");

    return optimizedMessages;
  }

  /** Applies summarization only - no sliding window */
  private List<Message> applySummarizationOnly(String conversationId, List<Message> allMessages) {
    try {
      log.info("Summarizing {} messages for conversation {}", allMessages.size(), conversationId);

      // Create summarization of entire conversation
      String conversationText = buildConversationText(allMessages);
      String summary = summarizeConversation(conversationText);

      // Create optimized list: just the summary as a system message
      List<Message> optimizedMessages = new ArrayList<>();
      optimizedMessages.add(new SystemMessage("Previous conversation summary: " + summary));

      // Update memory with optimized messages
      updateMemoryWithOptimizedMessages(conversationId, optimizedMessages);

      return optimizedMessages;

    } catch (Exception e) {
      log.error("Error during conversation summarization, returning original messages", e);
      return allMessages;
    }
  }

  /** Creates conversation summarization using AI */
  private String summarizeConversation(String conversationText) {
    try {
      String prompt = String.format(SUMMARIZATION_PROMPT, conversationText);

      String summary = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();

      log.info("Generated conversation summary: {} characters", summary.length());
      log.info("Summary content: {}", summary);
      return summary;

    } catch (Exception e) {
      log.error("Failed to generate conversation summary", e);
      return "Previous conversation context: Multiple topics were discussed. "
          + "Please refer to the user's current question for context.";
    }
  }

  /** Converts messages to text format for summarization */
  private String buildConversationText(List<Message> messages) {
    StringBuilder sb = new StringBuilder();

    for (Message message : messages) {
      if (message instanceof SystemMessage
          && message.getText().startsWith("Previous conversation summary:")) {
        // Include previous summaries to maintain context across multiple summarizations
        sb.append("Previous Context: ")
            .append(message.getText().substring("Previous conversation summary: ".length()))
            .append("\n\n");
      } else if (message instanceof UserMessage) {
        sb.append("User: ").append(message.getText()).append("\n");
      } else if (message instanceof AssistantMessage) {
        sb.append("Assistant: ").append(message.getText()).append("\n");
      }
      // Skip other SystemMessages (like system prompts) - they are not needed for summarization
    }

    return sb.toString();
  }

  /** Updates memory with optimized messages */
  private void updateMemoryWithOptimizedMessages(
      String conversationId, List<Message> optimizedMessages) {
    try {
      // Clear old memory
      chatMemory.clear(conversationId);

      // Add optimized messages
      for (Message message : optimizedMessages) {
        if (message instanceof UserMessage || message instanceof AssistantMessage) {
          chatMemory.add(conversationId, message);
        }
      }

      log.info(
          "Updated memory for conversation {} with {} optimized messages",
          conversationId,
          optimizedMessages.size());

    } catch (Exception e) {
      log.error("Failed to update memory with optimized messages", e);
    }
  }

  /** Estimates token count in messages */
  private int estimateTokenCount(List<Message> messages) {
    int totalChars = messages.stream().mapToInt(m -> m.getText().length()).sum();

    // Rough estimate: ~4 characters per token for English text
    return totalChars / 4;
  }

  /** Saves new messages to memory */
  public void addMessage(String conversationId, Message message) {
    // Increment attempt counter
    int attemptCount = messageCountMap.merge(conversationId, 1, Integer::sum);

    chatMemory.add(conversationId, message);

    // Debug: check size after addition
    int currentSize = chatMemory.get(conversationId).size();
    log.info(
        "Added message #{} to {}: {} (total messages now: {}, actual attempts: {})",
        attemptCount,
        conversationId,
        message.getClass().getSimpleName(),
        currentSize,
        attemptCount);

    // If size is not growing as expected
    if (currentSize < attemptCount) {
      log.warn(
          "WARNING: Memory size ({}) is less than attempt count ({}) - MessageWindowChatMemory may be limiting storage!",
          currentSize,
          attemptCount);
    }
  }
}
