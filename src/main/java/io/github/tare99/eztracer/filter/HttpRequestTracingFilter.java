package io.github.tare99.eztracer.filter;

import io.github.tare99.eztracer.mask.BodyMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class HttpRequestTracingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(HttpRequestTracingFilter.class);
  private static final String MDC_REQUEST_ID = "request_id";
  private static final String MDC_REQUEST_DURATION = "request_duration";
  private static final Set<String> IGNORED_PATHS =
      Set.of("actuator", "swagger", "open-api", "api-docs");

  private final BodyMasker bodyMasker;

  public HttpRequestTracingFilter(BodyMasker bodyMasker) {
    this.bodyMasker = bodyMasker;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    String requestUri = getOriginalUri(request);
    String requestId = UUID.randomUUID().toString();
    long startNanos = 0;
    try {
      if (shouldNotIgnore(requestUri)) {
        MDC.put(MDC_REQUEST_ID, requestId);
        String requestBody =
            bodyMasker.mask(
                new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info("Request START {} {} | Body: {}", request.getMethod(), requestUri, requestBody);
        startNanos = System.nanoTime();
      }
      chain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
      if (shouldNotIgnore(requestUri)) {
        MDC.put(MDC_REQUEST_DURATION, String.valueOf(durationMs));
        String responseBody =
            bodyMasker.mask(
                new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info(
            "Request END {} {} | Status: {} | Body: {} | Time: {}ms",
            request.getMethod(),
            requestUri,
            wrappedResponse.getStatus(),
            responseBody,
            durationMs);
        MDC.remove(MDC_REQUEST_DURATION);
        MDC.remove(MDC_REQUEST_ID);
      }
      wrappedResponse.copyBodyToResponse();
    }
  }

  private boolean shouldNotIgnore(String uri) {
    return IGNORED_PATHS.stream().noneMatch(uri::contains);
  }

  private String getOriginalUri(HttpServletRequest request) {
    String originalUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    return originalUri != null ? originalUri : request.getRequestURI();
  }
}
