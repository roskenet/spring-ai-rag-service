package com.zalando.rag.config;

import com.zalando.rag.service.TokenInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Exclude from test profile
public class DynamicTokenConfiguration {

  @Value("${ZTOKEN:}")
  private String defaultToken;

  @Bean
  @ConditionalOnProperty(name = "spring.ai.openai.api-key")
  public RestClientCustomizer openAiRestClientCustomizer(TokenInfoService tokenInfoService) {
    return restClientBuilder ->
        restClientBuilder.requestInterceptor(
            new DynamicTokenInterceptor(tokenInfoService, defaultToken));
  }

  /** Interceptor that dynamically sets the Authorization header based on X-TokenInfo-Forward */
  public static class DynamicTokenInterceptor implements ClientHttpRequestInterceptor {
    private final TokenInfoService tokenInfoService;
    private final String fallbackToken;

    public DynamicTokenInterceptor(TokenInfoService tokenInfoService, String fallbackToken) {
      this.tokenInfoService = tokenInfoService;
      this.fallbackToken = fallbackToken;
    }

    @Override
    public org.springframework.http.client.ClientHttpResponse intercept(
        org.springframework.http.HttpRequest request,
        byte[] body,
        org.springframework.http.client.ClientHttpRequestExecution execution)
        throws java.io.IOException {

      long startTime = System.currentTimeMillis();

      // Get effective token (from X-TokenInfo-Forward or fallback)
      String effectiveToken = tokenInfoService.getEffectiveToken(fallbackToken);

      // Log request details before execution
      logRequestDetails(request, body, effectiveToken);

      // Only set Authorization header if we have a valid token
      if (effectiveToken != null && !effectiveToken.trim().isEmpty()) {
        request.getHeaders().set("Authorization", "Bearer " + effectiveToken);
      } else {
        // In staging this is a critical error
        log.error("No valid token available for OpenAI API request to: {}", request.getURI());
      }

      try {
        // Continue with the request
        org.springframework.http.client.ClientHttpResponse response =
            execution.execute(request, body);

        // Log response details after execution
        logResponseDetails(response, startTime);

        return response;

      } catch (Exception e) {
        logRequestFailure(e, startTime);
        throw e;
      }
    }

    private void logRequestDetails(
        org.springframework.http.HttpRequest request, byte[] body, String effectiveToken) {
      if (!log.isDebugEnabled()) return;

      log.debug("=== zLLM API Request ===");
      log.debug("Method: {}", request.getMethod());
      log.debug("URI: {}", request.getURI());
      log.debug("Headers (before token injection): {}", request.getHeaders());

      if (body != null && body.length > 0) {
        String bodyString = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        log.debug("Request Body: {}", bodyString);
      } else {
        log.debug("Request Body: <empty>");
      }

      if (effectiveToken != null && !effectiveToken.trim().isEmpty()) {
        String maskedToken = maskToken(effectiveToken);
        log.debug("Using token: {}", maskedToken);
      } else {
        log.debug("No token available");
      }
    }

    private void logResponseDetails(
        org.springframework.http.client.ClientHttpResponse response, long startTime) {
      if (!log.isDebugEnabled()) return;

      long responseTime = System.currentTimeMillis() - startTime;
      try {
        log.debug("=== zLLM API Response ===");
        log.debug("Status: {} {}", response.getStatusCode().value(), response.getStatusText());
        log.debug("Response Headers: {}", response.getHeaders());
        log.debug("Response Time: {}ms", responseTime);
      } catch (Exception e) {
        log.debug("Failed to log response details: {}", e.getMessage());
      }
    }

    private void logRequestFailure(Exception e, long startTime) {
      long responseTime = System.currentTimeMillis() - startTime;
      log.error("zLLM API request failed after {}ms: {}", responseTime, e.getMessage(), e);
    }

    /** Masks the token for secure logging - shows only first and last 4 characters */
    private String maskToken(String token) {
      if (token == null || token.length() <= 8) {
        return "****";
      }
      return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
  }
}
