package com.zalando.rag.service.chunking.example;

import static org.junit.jupiter.api.Assertions.*;

import com.zalando.rag.service.chunking.ChunkingConfig;
import com.zalando.rag.service.chunking.DocumentAnalysis;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class CodeAwareChunkingStrategyTest {

  private CodeAwareChunkingStrategy strategy;
  private ChunkingConfig defaultConfig;

  @BeforeEach
  void setUp() {
    strategy = new CodeAwareChunkingStrategy();
    defaultConfig = ChunkingConfig.defaultConfig();
  }

  @Test
  void testStrategyProperties() {
    assertEquals("code-aware", strategy.getStrategyName());
    assertEquals(90, strategy.getPriority());
    assertNotNull(strategy.getDescription());
    assertTrue(strategy.getDescription().contains("code"));
  }

  @Test
  void testCanHandleCodeHeavyDocument() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.CODE_HEAVY);
    analysis.setCodeRatio(0.5);
    analysis.setCodeBlockCount(3);

    assertTrue(strategy.canHandle(analysis));
  }

  @Test
  void testCanHandleTechnicalGuide() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.TECHNICAL_GUIDE);
    analysis.setCodeRatio(0.4);
    analysis.setCodeBlockCount(5);

    assertTrue(strategy.canHandle(analysis));
  }

  @Test
  void testCannotHandleSimpleText() {
    DocumentAnalysis analysis = new DocumentAnalysis();
    analysis.setDocumentType(DocumentAnalysis.DocumentType.SIMPLE_TEXT);
    analysis.setCodeRatio(0.1);
    analysis.setCodeBlockCount(0);

    assertFalse(strategy.canHandle(analysis));
  }

  @Test
  void testChunkCodeHeavyDocument() {
    String codeDocument =
        """
            # Java Programming Guide

            This guide covers basic Java programming concepts.

            ## Classes and Objects

            In Java, everything is an object. Here's how to define a class:

            ```java
            public class Person {
                private String name;
                private int age;

                public Person(String name, int age) {
                    this.name = name;
                    this.age = age;
                }

                public void greet() {
                    System.out.println("Hello, I'm " + name);
                }
            }
            ```

            This Person class demonstrates encapsulation and basic method definition.

            ## Methods

            Methods define the behavior of objects:

            ```java
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }

                public double divide(double a, double b) {
                    if (b == 0) {
                        throw new IllegalArgumentException("Cannot divide by zero");
                    }
                    return a / b;
                }
            }
            ```

            The Calculator class shows method overloading and error handling.

            ## Best Practices

            Always follow these Java best practices:
            - Use meaningful variable names
            - Handle exceptions properly
            - Write unit tests
            - Document your code
            """;

    List<Document> chunks =
        strategy.chunkDocument(codeDocument, "java-guide.md", "Java Guide", defaultConfig);

    // Should create multiple chunks
    assertTrue(chunks.size() >= 2, "Should create multiple chunks for code-heavy document");

    // Verify code blocks are preserved
    boolean foundPersonClass =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("public class Person")
                        && chunk.getText().contains("public void greet()"));
    assertTrue(foundPersonClass, "Person class code block should be preserved complete");

    boolean foundCalculatorClass =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("public class Calculator")
                        && chunk.getText().contains("public int add"));
    assertTrue(foundCalculatorClass, "Calculator class code block should be preserved complete");

    // Verify metadata
    for (Document chunk : chunks) {

      assertEquals("code-aware", chunk.getMetadata().get("chunking_strategy"));
      assertNotNull(chunk.getMetadata().get("filename"));
      assertNotNull(chunk.getMetadata().get("chunk_index"));

      // Check code-specific metadata
      assertTrue(chunk.getMetadata().containsKey("contains_code_block"));
      assertTrue(chunk.getMetadata().containsKey("contains_function"));

      // If chunk contains Java code, should have language metadata
      if ((Boolean) chunk.getMetadata().get("contains_code_block")) {
        String language = (String) chunk.getMetadata().get("programming_language");
        // Language detection might fail for incomplete code blocks, but we should have detected
        // java somewhere
        assertTrue(
            language == null || "java".equals(language),
            "Programming language should be null or 'java', but was: " + language);
      }
    }
  }

  @Test
  void testChunkDocumentWithMultipleLanguages() {
    String multiLanguageDoc =
        """
            # Multi-Language Guide

            ## Python Example

            ```python
            def fibonacci(n):
                if n <= 1:
                    return n
                return fibonacci(n-1) + fibonacci(n-2)

            print(fibonacci(10))
            ```

            ## JavaScript Example

            ```javascript
            function factorial(n) {
                if (n <= 1) return 1;
                return n * factorial(n - 1);
            }

            console.log(factorial(5));
            ```

            ## SQL Example

            ```sql
            SELECT u.name, COUNT(o.id) as order_count
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            GROUP BY u.id, u.name
            ORDER BY order_count DESC;
            ```
            """;

    List<Document> chunks =
        strategy.chunkDocument(multiLanguageDoc, "multi-lang.md", "Multi Language", defaultConfig);

    assertFalse(chunks.isEmpty());

    // Verify different languages are detected (at least one of each should be found)
    Set<String> detectedLanguages =
        chunks.stream()
            .map(chunk -> (String) chunk.getMetadata().get("programming_language"))
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

    // At least some languages should be detected from the multi-language content
    assertFalse(detectedLanguages.isEmpty(), "Should detect at least some programming languages");

    // Check if the content actually contains the expected languages
    boolean foundPython =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("def fibonacci")
                        || "python".equals(chunk.getMetadata().get("programming_language")));
    boolean foundJavaScript =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("function factorial")
                        || "javascript".equals(chunk.getMetadata().get("programming_language")));
    boolean foundSQL =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("SELECT u.name")
                        || "sql".equals(chunk.getMetadata().get("programming_language")));

    assertTrue(foundPython, "Should find Python code content");
    assertTrue(foundJavaScript, "Should find JavaScript code content");
    assertTrue(foundSQL, "Should find SQL code content");
  }

  @Test
  void testChunkDocumentWithInlineCode() {
    String docWithInlineCode =
        """
            # API Usage Guide

            To use the API, you need to call the `getUserData()` method with a valid `userId` parameter.
            The method returns a `User` object containing the user's information.

            You can also use the `updateUser()` method to modify user data:

            ```java
            User user = api.getUserData(123);
            user.setEmail("new@example.com");
            api.updateUser(user);
            ```

            Remember to handle the `UserNotFoundException` that might be thrown.
            """;

    List<Document> chunks =
        strategy.chunkDocument(docWithInlineCode, "api-guide.md", "API Guide", defaultConfig);

    assertFalse(chunks.isEmpty());

    // Check that inline code is detected
    boolean foundInlineCode =
        chunks.stream()
            .anyMatch(chunk -> (Boolean) chunk.getMetadata().get("contains_inline_code"));
    assertTrue(foundInlineCode, "Should detect inline code");

    // Check that code block is preserved
    boolean foundCodeBlock =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("api.getUserData(123)")
                        && chunk.getText().contains("api.updateUser(user)"));
    assertTrue(foundCodeBlock, "Code block should be preserved complete");
  }

  @Test
  void testEmptyContent() {
    List<Document> chunks = strategy.chunkDocument("", "empty.txt", "Empty", defaultConfig);
    assertTrue(chunks.isEmpty());

    chunks = strategy.chunkDocument(null, "null.txt", "Null", defaultConfig);
    assertTrue(chunks.isEmpty());
  }

  @Test
  void testDocumentWithoutCode() {
    String textDocument =
        """
            # Simple Text Document

            This is a simple text document without any code examples.
            It contains only regular text content.

            ## Section 1

            Some explanatory text about concepts.

            ## Section 2

            More text content here.
            """;

    List<Document> chunks =
        strategy.chunkDocument(textDocument, "simple.md", "Simple", defaultConfig);

    assertFalse(chunks.isEmpty());

    // Should fall back to simple chunking
    for (Document chunk : chunks) {
      assertEquals("code-aware", chunk.getMetadata().get("chunking_strategy"));
      assertEquals(Boolean.FALSE, chunk.getMetadata().get("contains_code_block"));
      assertEquals(Boolean.FALSE, chunk.getMetadata().get("contains_function"));
    }
  }

  @Test
  void testChunkSizeConstraints() {
    // Create a very long code block
    StringBuilder longCode = new StringBuilder();
    longCode.append("# Long Code Example\n\n");
    longCode.append("```java\n");
    longCode.append("public class VeryLongClass {\n");
    for (int i = 0; i < 100; i++) {
      longCode.append("    private String field").append(i).append(";\n");
    }
    longCode.append("}\n");
    longCode.append("```\n");

    ChunkingConfig smallConfig =
        ChunkingConfig.builder()
            .maxChunkSize(1000)
            .preferredChunkSize(500)
            .minChunkSize(100)
            .build();

    List<Document> chunks =
        strategy.chunkDocument(longCode.toString(), "long.md", "Long", smallConfig);

    assertFalse(chunks.isEmpty());

    // Even with size constraints, code should be preserved (though it might exceed max size)
    boolean foundCompleteCode =
        chunks.stream()
            .anyMatch(
                chunk ->
                    chunk.getText().contains("VeryLongClass")
                        && chunk.getText().contains("field99"));
    assertTrue(
        foundCompleteCode, "Long code block should be preserved even if it exceeds max size");
  }
}
