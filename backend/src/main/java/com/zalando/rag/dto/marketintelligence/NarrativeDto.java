package com.zalando.rag.dto.marketintelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NarrativeDto {
  private String intro;
  private List<NarrativeHookDto> hooks;
  private List<String> watchouts;
  private List<String> leadWith;
  private List<String> openers;
}
