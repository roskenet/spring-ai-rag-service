package com.zalando.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoService {

  private final ObjectMapper objectMapper;

  /**
   * Extracts the access_token from X-TokenInfo-Forward header
   *
   * @return access_token if found and valid, null otherwise
   */
  public String getAccessTokenFromHeader() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        log.debug("No request attributes available");
        return null;
      }

      HttpServletRequest request = attributes.getRequest();
      String tokenInfoHeader = request.getHeader("X-TokenInfo-Forward");

      if (tokenInfoHeader == null || tokenInfoHeader.isEmpty()) {
        log.debug("X-TokenInfo-Forward header not present in request");
        return null;
      }

      log.debug("X-TokenInfo-Forward header received: {}", tokenInfoHeader);

      // Parse as JSON and extract access_token
      try {
        JsonNode jsonNode = objectMapper.readTree(tokenInfoHeader);
        JsonNode accessTokenNode = jsonNode.get("access_token");

        if (accessTokenNode != null && !accessTokenNode.isNull()) {
          String accessToken = accessTokenNode.asText();
          if (accessToken != null && !accessToken.isEmpty()) {
            log.info("Successfully extracted access_token from X-TokenInfo-Forward header");
            return accessToken;
          }
        }

        log.debug("No access_token field found in X-TokenInfo-Forward header");
        return null;

      } catch (Exception jsonException) {
        log.warn(
            "Failed to parse X-TokenInfo-Forward header as JSON: {}", jsonException.getMessage());
        return null;
      }

    } catch (Exception e) {
      log.warn("Error extracting access_token from X-TokenInfo-Forward header", e);
      return null;
    }
  }

  /**
   * Gets the effective token to use for LLM requests. Prefers access_token from X-TokenInfo-Forward
   * header, falls back to ZTOKEN
   *
   * @param fallbackToken the fallback token (usually ZTOKEN from config)
   * @return the token to use for API requests
   */
  public String getEffectiveToken(String fallbackToken) {
    String accessToken = getAccessTokenFromHeader();
    if (accessToken != null) {
      log.debug("Using access_token from X-TokenInfo-Forward header for LLM request");
      return accessToken;
    }

    // Check if fallback token is valid
    if (fallbackToken != null && !fallbackToken.trim().isEmpty()) {
      log.debug("Using fallback token (ZTOKEN) for LLM request");
      return fallbackToken;
    }

    log.debug("No valid token available - neither X-TokenInfo-Forward nor ZTOKEN");
    return null;
  }
}
