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

      // Get effective token (from X-TokenInfo-Forward or fallback)
      String effectiveToken = tokenInfoService.getEffectiveToken(fallbackToken);

      // Only set Authorization header if we have a valid token
      if (effectiveToken != null && !effectiveToken.trim().isEmpty()) {
        request.getHeaders().set("Authorization", "Bearer " + effectiveToken);
      } else {
        // In staging this is a critical error
        log.error("No valid token available for OpenAI API request to: {}", request.getURI());
      }

      // Continue with the request
      return execution.execute(request, body);
    }
  }
}
