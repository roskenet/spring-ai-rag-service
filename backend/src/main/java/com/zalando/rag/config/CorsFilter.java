package com.zalando.rag.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorsFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String origin = request.getHeader("Origin");

    // Allow requests from allowed origins or if no origin header (same-origin requests)
    if (origin != null && isAllowedOrigin(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
    } else if (origin == null) {
      // For same-origin requests or requests without Origin header
      response.setHeader("Access-Control-Allow-Origin", "*");
    }

    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader("Access-Control-Allow-Credentials", "false");
    // Expose headers that frontend might need
    response.setHeader("Access-Control-Expose-Headers", "Content-Type");

    // Handle preflight requests
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      log.debug("Handling CORS preflight request from origin: {}", origin);
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    chain.doFilter(req, res);
  }

  private boolean isAllowedOrigin(String origin) {
    if (origin == null) {
      return false;
    }

    // Allow localhost for development
    if (origin.matches("https?://localhost(:\\d+)?")
        || origin.matches("https?://127\\.0\\.0\\.1(:\\d+)?")) {
      return true;
    }

    log.warn("CORS request from disallowed origin: {}", origin);
    return false;
  }
}
