package com.zalando.rag.dto.marketintelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NarrativeHookDto {
  private String product;
  private String painPoint;
  private String hook;
}
