package com.zalando.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zalando.rag.dto.marketintelligence.BrandDto;
import com.zalando.rag.dto.marketintelligence.NewsCategoriesDto;
import com.zalando.rag.dto.marketintelligence.NarrativeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketIntelligenceService {

  private final ChatModel chatModel;
  private final ObjectMapper objectMapper;

  private static final String NARRATIVE_SYSTEM_PROMPT =
      """
      You are a senior Account Development Manager at ZEOS (Zalando's logistics-as-a-service business).
      Your job is to prepare sales intelligence for approaching European fashion and retail brands.
      ZEOS products: ZEOS Fulfilment, ZFS (Zalando Fulfilment Service), ZEOS One, ZEOS Shipping, ZEOS Returns, ZEOS Content Services.
      Always respond with valid JSON only. No markdown, no code fences, no explanation — just the raw JSON object.
      """;

  private static final String NEWS_SYSTEM_PROMPT =
      """
      You are a market intelligence analyst specialising in European fashion and retail ecommerce.
      Your knowledge extends to early 2025. Always prioritise the most recent information you have — include 2024 and 2025 signals even if your data is partial or incomplete.
      Do not default to older data out of caution. If you know something happened in 2024 or 2025, include it with the appropriate date.
      Always respond with valid JSON only. No markdown, no code fences, no explanation — just the raw JSON object.
      """;

  public NarrativeDto generateNarrative(BrandDto brand) {
    String marketsStr = brand.getMarkets() != null
        ? brand.getMarkets().stream().map(m -> m.getCountry()).reduce((a, b) -> a + ", " + b).orElse("EU")
        : "EU";

    String volumeMixStr = brand.getVolumeMix() != null
        ? String.format("PPB %d units, Marketplaces %d units, Own.com %d units",
            brand.getVolumeMix().getPpb(),
            brand.getVolumeMix().getMarketplaces(),
            brand.getVolumeMix().getOwnCom())
        : "unknown";

    String userPrompt = String.format("""
        Generate a sales narrative for %s.

        Brand context:
        - Parent: %s
        - HQ: %s
        - Online revenue: €%dM
        - Logistics: %s%s
        - Warehouse: %s
        - Segment: %s
        - Priority: %s
        - Markets: %s
        - Volume mix: %s
        - Status: %s

        Return a JSON object with this exact structure:
        {
          "intro": "2-3 sentence executive summary of the ZEOS opportunity for this brand",
          "hooks": [
            { "product": "ZEOS product name", "painPoint": "specific pain point for this brand", "hook": "1-2 sentence pitch" }
          ],
          "watchouts": ["watch-out 1", "watch-out 2"],
          "leadWith": ["product1", "product2"],
          "openers": ["conversation opener 1", "conversation opener 2", "conversation opener 3"]
        }

        Provide exactly 3 hooks, 2 watchouts, 2 lead-with products, 3 openers. Be specific to this brand's situation.
        """,
        brand.getName(),
        brand.getParentCompany(),
        brand.getHq(),
        (long) (brand.getOnlineRevenue() / 1_000_000),
        brand.getLogistics(),
        brand.getThreePl() != null ? " (" + brand.getThreePl() + ")" : "",
        brand.getWarehouseLocation() != null ? brand.getWarehouseLocation() : "unknown",
        brand.getSegment(),
        brand.getPrioritySegment(),
        marketsStr,
        volumeMixStr,
        brand.getExistingOrPotential()
    );

    String response = ChatClient.builder(chatModel)
        .defaultSystem(NARRATIVE_SYSTEM_PROMPT)
        .build()
        .prompt()
        .user(userPrompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(extractJson(response), NarrativeDto.class);
    } catch (Exception e) {
      log.error("Failed to parse narrative response for brand {}: {}", brand.getName(), e.getMessage());
      throw new RuntimeException("Failed to parse narrative response", e);
    }
  }

  public NewsCategoriesDto generateNews(BrandDto brand) {
    String marketsStr = brand.getMarkets() != null
        ? brand.getMarkets().stream().map(m -> m.getCountry()).reduce((a, b) -> a + ", " + b).orElse("EU")
        : "EU";

    String userPrompt = String.format("""
        You are a market intelligence analyst. Using your training knowledge, provide detailed intelligence signals for %s and its parent company %s.

        Brand context:
        - HQ: %s
        - Online revenue: €%dM
        - Logistics: %s%s
        - Markets: %s
        - Segment: %s

        Important notes:
        - Search your knowledge for both "%s" AND "%s" — they may appear under either name in news
        - Be specific: include figures, names, locations where you know them
        - Only include what you actually know — do not invent events
        - If a category has no known signals, write one sentence on what an ADM should look for
        - "date": provide the most specific date you know — year, quarter, or month-year (e.g. "2025-02", "2024-Q3", "2025"). Leave blank only if truly unknown
        - "source": real publication name if known (e.g. "FashionNetwork", "Reuters", "Company IR page"), otherwise ""
        - Prioritise 2024 and 2025 signals — use them even if your knowledge is partial. Only fall back to 2023 if you have nothing more recent

        Return a JSON object with 1-3 items per category:
        {
          "pressReleases": [{ "text": "...", "date": "", "source": "" }],
          "mandA": [{ "text": "...", "date": "", "source": "" }],
          "ecommerceLogistics": [{ "text": "...", "date": "", "source": "" }],
          "financialReports": [{ "text": "...", "date": "", "source": "" }],
          "ecommerceNews": [{ "text": "...", "date": "", "source": "" }],
          "managementChanges": [{ "text": "...", "date": "", "source": "" }]
        }
        """,
        brand.getName(),
        brand.getParentCompany(),
        brand.getHq(),
        (long) (brand.getOnlineRevenue() / 1_000_000),
        brand.getLogistics(),
        brand.getThreePl() != null ? " via " + brand.getThreePl() : "",
        marketsStr,
        brand.getSegment(),
        brand.getName(),
        brand.getParentCompany()
    );

    String response = ChatClient.builder(chatModel)
        .defaultSystem(NEWS_SYSTEM_PROMPT)
        .build()
        .prompt()
        .user(userPrompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(extractJson(response), NewsCategoriesDto.class);
    } catch (Exception e) {
      log.error("Failed to parse news response for brand {}: {}", brand.getName(), e.getMessage());
      throw new RuntimeException("Failed to parse news response", e);
    }
  }

  private String extractJson(String text) {
    if (text == null) return "{}";
    // Strip fenced code blocks if model wraps in markdown
    int fenceStart = text.indexOf("```");
    if (fenceStart != -1) {
      int jsonStart = text.indexOf('\n', fenceStart) + 1;
      int fenceEnd = text.lastIndexOf("```");
      if (fenceEnd > jsonStart) {
        return text.substring(jsonStart, fenceEnd).trim();
      }
    }
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start != -1 && end != -1) return text.substring(start, end + 1);
    return text.trim();
  }
}
