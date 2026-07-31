package com.zalando.rag.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (request instanceof HttpServletRequest httpRequest) {
      // Only log for API endpoints to avoid cluttering logs with actuator/static resources
      String requestURI = httpRequest.getRequestURI();
      if (requestURI.startsWith("/api/") && log.isDebugEnabled()) {
        logIncomingRequest(httpRequest);
      }
    }

    chain.doFilter(request, response);
  }

  private void logIncomingRequest(HttpServletRequest request) {
    log.debug("=== Incoming Request to {} {} ===", request.getMethod(), request.getRequestURI());

    // Log specific headers we care about
    String authHeader = request.getHeader("Authorization");
    String userAgent = request.getHeader("User-Agent");
    String contentType = request.getHeader("Content-Type");

    log.debug("Authorization: {}", authHeader != null ? maskToken(authHeader) : "null");
    log.debug("Content-Type: {}", contentType != null ? contentType : "null");
    log.debug("User-Agent: {}", userAgent != null ? userAgent : "null");

    // Log all headers if trace level is enabled
    if (log.isTraceEnabled()) {
      log.trace("=== All Headers ===");
      Collections.list(request.getHeaderNames())
          .forEach(
              headerName -> {
                String value = request.getHeader(headerName);
                // Mask authorization-related headers for security
                if (headerName.toLowerCase().contains("auth")
                    || headerName.toLowerCase().contains("token")) {
                  value = maskToken(value);
                }
                log.trace("{}: {}", headerName, value);
              });
      log.trace("=== End All Headers ===");
    }

    log.debug("=== End Incoming Request ===");
  }

  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "****";
    }
    if (token.startsWith("Bearer ")) {
      String actualToken = token.substring(7);
      if (actualToken.length() <= 8) {
        return "Bearer ****";
      }
      return "Bearer "
          + actualToken.substring(0, 4)
          + "..."
          + actualToken.substring(actualToken.length() - 4);
    }
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }
}
