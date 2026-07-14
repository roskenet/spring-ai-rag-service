package com.zalando.rag.dto.marketintelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketDto {
  private String country;
  private long webTraffic;
  private double estimatedGmv;
  private long estimatedShippedItems;
  private List<String> carriers;
  private List<String> paymentOptions;
  private List<String> deliveryOptions;
}
