package com.zalando.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoService {

  private final ObjectMapper objectMapper;

  @Value("${rag.token.require-header-token:false}")
  private boolean requireHeaderToken;

  /**
   * Extracts the JWT token from Authorization header
   *
   * @return JWT token if found and valid, null otherwise
   */
  public String getAccessTokenFromAuthorizationHeader() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        log.debug("No request attributes available");
        return null;
      }

      HttpServletRequest request = attributes.getRequest();
      String authorizationHeader = request.getHeader("Authorization");

      if (authorizationHeader == null || authorizationHeader.isEmpty()) {
        log.debug("Authorization header not present in request");
        return null;
      }

      // Extract Bearer token
      if (authorizationHeader.startsWith("Bearer ")) {
        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        if (token != null && !token.isEmpty()) {
          log.info("Successfully extracted JWT token from Authorization header");
          return token;
        }
      }

      log.debug("Authorization header does not contain a valid Bearer token");
      return null;

    } catch (Exception e) {
      log.warn("Error extracting JWT token from Authorization header", e);
      return null;
    }
  }

  /**
   * Extracts the access_token from X-TokenInfo-Forward header (LEGACY - likely not working)
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
   * Gets the effective token to use for LLM requests. Prefers JWT token from Authorization header,
   * falls back to ZTOKEN
   *
   * @param fallbackToken the fallback token (usually ZTOKEN from config)
   * @return the token to use for API requests
   */
  public String getEffectiveToken(String fallbackToken) {
    // First try to get JWT token from Authorization header (set by OAuth flow)
    String jwtToken = getAccessTokenFromAuthorizationHeader();
    if (jwtToken != null) {
      log.info("Using JWT token from Authorization header for LLM request");
      return jwtToken;
    }

    // Legacy: Try X-TokenInfo-Forward header (likely won't work as it contains claims, not token)
    String accessToken = getAccessTokenFromHeader();
    if (accessToken != null) {
      log.info("Using access_token from X-TokenInfo-Forward header for LLM request");
      return accessToken;
    }

    // In staging environment token MUST come from header
    if (requireHeaderToken) {
      log.warn(
          "Authorization header with Bearer token is required in staging but not present - rejecting request");
      return null;
    }

    // Use fallback token (ZTOKEN) for local development
    if (fallbackToken != null
        && !fallbackToken.trim().isEmpty()
        && !"dummy-token-will-be-replaced-by-interceptor".equals(fallbackToken)) {
      log.debug("Using fallback token (ZTOKEN) for LLM request");
      return fallbackToken;
    }

    log.debug("No valid token available - neither Authorization header nor ZTOKEN");
    return null;
  }

  /**
   * Extracts user information from X-TokenInfo-Forward header This header contains decoded JWT
   * claims like uid, realm, scope
   *
   * @return user context information for logging/audit purposes
   */
  public String getUserContext() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        return "no-request-context";
      }

      HttpServletRequest request = attributes.getRequest();
      String tokenInfoHeader = request.getHeader("X-TokenInfo-Forward");

      if (tokenInfoHeader == null || tokenInfoHeader.isEmpty()) {
        return "no-token-info";
      }

      // Parse token info to extract user details
      JsonNode jsonNode = objectMapper.readTree(tokenInfoHeader);
      String uid = jsonNode.has("uid") ? jsonNode.get("uid").asText() : "unknown";
      String realm = jsonNode.has("realm") ? jsonNode.get("realm").asText() : "unknown";

      return String.format("user=%s,realm=%s", uid, realm);

    } catch (Exception e) {
      log.debug("Failed to extract user context from X-TokenInfo-Forward header", e);
      return "context-extraction-failed";
    }
  }
}
