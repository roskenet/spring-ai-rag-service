package com.zalando.rag.dto.marketintelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrandDto {
  private String id;
  private String name;
  private String parentCompany;
  private String prioritySegment;
  private String segment;
  private double onlineRevenue;
  private String existingOrPotential;
  private VolumeMixDto volumeMix;
  private String logistics;
  private String threePl;
  private String rating;
  private String warehouseLocation;
  private String hq;
  private long globalWebTraffic;
  private long euWebTraffic;
  private double estimatedOwnEcomGmv;
  private long estimatedOwnEcomShippedItems;
  private List<MarketDto> markets;
}
