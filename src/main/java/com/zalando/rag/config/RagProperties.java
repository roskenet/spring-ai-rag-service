package com.zalando.rag.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zeos.rag")
@Data
public class RagProperties {

  private int chunkSize = 1000;
  private int chunkOverlap = 200;
  private int maxResults = 5;
  private double similarityThreshold = 0.7;
  private String maxFileSize = "10MB";
  private List<String> allowedFileTypes = List.of("md", "markdown", "txt");
}
