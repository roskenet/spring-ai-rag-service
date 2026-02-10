package com.zalando.rag.config;

import com.zalando.rag.service.TokenInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DynamicTokenConfiguration {

  @Value("${ZTOKEN:test-token}")
  private String defaultToken;

  @Bean
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

      // Set Authorization header
      request.getHeaders().set("Authorization", "Bearer " + effectiveToken);

      // Continue with the request
      return execution.execute(request, body);
    }
  }
}
