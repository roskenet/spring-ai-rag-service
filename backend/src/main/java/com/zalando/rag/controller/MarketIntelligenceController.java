package com.zalando.rag.controller;

import com.zalando.rag.dto.marketintelligence.BrandDto;
import com.zalando.rag.dto.marketintelligence.NarrativeDto;
import com.zalando.rag.dto.marketintelligence.NewsCategoriesDto;
import com.zalando.rag.service.MarketIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-intelligence")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketIntelligenceController {

  private final MarketIntelligenceService marketIntelligenceService;

  @PostMapping("/narrative")
  public ResponseEntity<NarrativeDto> generateNarrative(@RequestBody BrandDto brand) {
    try {
      log.info("Generating sales narrative for brand: {}", brand.getName());
      NarrativeDto narrative = marketIntelligenceService.generateNarrative(brand);
      return ResponseEntity.ok(narrative);
    } catch (Exception e) {
      log.error("Error generating narrative for brand {}: {}", brand.getName(), e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PostMapping("/news")
  public ResponseEntity<NewsCategoriesDto> generateNews(@RequestBody BrandDto brand) {
    try {
      log.info("Generating market intelligence news for brand: {}", brand.getName());
      NewsCategoriesDto news = marketIntelligenceService.generateNews(brand);
      return ResponseEntity.ok(news);
    } catch (Exception e) {
      log.error("Error generating news for brand {}: {}", brand.getName(), e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
