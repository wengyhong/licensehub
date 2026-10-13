package com.weng.licensehub.activation.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

// This filter:
// - Runs once per request.
// - Limits only POST /api/activations and its subpaths.
// - Leaves products, licenses, authentication, Swagger, and health untouched.
// - Returns 429 Too Many Requests before reaching the controller.
// - Uses getRemoteAddr() rather than trusting a spoofable forwarded-IP header.
// Behind a trusted reverse proxy, client-IP forwarding requires separate proxy configuration. For
// the current direct Docker/local deployment, getRemoteAddr() is appropriate.

public class ActivationRateLimitFilter extends OncePerRequestFilter {

  private final ActivationRateLimiter rateLimiter;

  public ActivationRateLimitFilter(ActivationRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {

    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();

    return !path.equals("/api/activations") && !path.startsWith("/api/activations/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String clientAddress = request.getRemoteAddr();

    if (rateLimiter.tryConsume(clientAddress)) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Retry-After", "60");

    response
        .getWriter()
        .write(
            """
                {
                  "title": "Too many requests",
                  "status": 429,
                  "detail": "Activation request limit exceeded"
                }
                """);
  }
}
