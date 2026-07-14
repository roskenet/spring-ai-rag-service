package com.zalando.rag.dto.marketintelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsCategoriesDto {
  private List<NewsItemDto> pressReleases;
  private List<NewsItemDto> mandA;
  private List<NewsItemDto> ecommerceLogistics;
  private List<NewsItemDto> financialReports;
  private List<NewsItemDto> ecommerceNews;
  private List<NewsItemDto> managementChanges;
}
