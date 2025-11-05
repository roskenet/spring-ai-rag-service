package com.zalando.rag.service.chunking.util;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for safely handling metadata in Document objects. Spring AI Document requires that
 * metadata values cannot be null.
 */
@Slf4j
public class MetadataUtil {

  /**
   * Safely adds a value to metadata, ensuring no null values are added. Spring AI Document requires
   * that metadata values cannot be null.
   *
   * @param metadata the metadata map to add to
   * @param key the metadata key
   * @param value the value to add (null values will be converted to appropriate defaults)
   */
  public static void safeMetadataPut(Map<String, Object> metadata, String key, Object value) {
    if (value != null) {
      metadata.put(key, value);
    } else {
      // For certain optional keys, don't add them if null
      if (isOptionalKey(key)) {
        log.debug("Skipping null value for optional metadata key '{}'", key);
        return;
      }

      // Log warning about null value being replaced
      log.debug("Null value detected for metadata key '{}', using default", key);
      // Provide appropriate default based on key
      if (key.contains("size")
          || key.contains("length")
          || key.contains("complexity")
          || key.contains("time")
          || key.contains("index")
          || key.contains("count")) {
        metadata.put(key, 0);
      } else if (key.contains("ratio") || key.contains("rate") || key.contains("score")) {
        metadata.put(key, 0.0);
      } else if (key.contains("preserve") || key.contains("maintain") || key.contains("contains")) {
        metadata.put(key, false);
      } else {
        metadata.put(key, "unknown");
      }
    }
  }

  /** Checks if a metadata key is optional and can be omitted when null. */
  private static boolean isOptionalKey(String key) {
    return key.equals("programming_language")
        || key.equals("custom_delimiter")
        || key.equals("section_title");
  }

  /** Safely adds a string value to metadata, with a specific default for null strings. */
  public static void safeMetadataPut(
      Map<String, Object> metadata, String key, String value, String defaultValue) {
    if (value != null && !value.trim().isEmpty()) {
      metadata.put(key, value);
    } else {
      metadata.put(key, defaultValue != null ? defaultValue : "unknown");
    }
  }
}
