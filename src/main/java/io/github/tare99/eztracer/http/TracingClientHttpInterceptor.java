package io.github.tare99.eztracer.http;

import io.github.tare99.eztracer.mask.BodyMasker;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TracingClientHttpInterceptor implements ClientHttpRequestInterceptor {

  public static final String MDC_EXTERNAL_API_DURATION = "external_api_duration";
  private static final Logger log = LoggerFactory.getLogger(TracingClientHttpInterceptor.class);
  private static final BodyMasker NO_OP_MASKER = new BodyMasker(List.of(), "");
  private final String label;
  private final BodyMasker bodyMasker;

  public TracingClientHttpInterceptor() {
    this.label = "Third party";
    this.bodyMasker = NO_OP_MASKER;
  }

  public TracingClientHttpInterceptor(String label) {
    this.label = label;
    this.bodyMasker = NO_OP_MASKER;
  }

  public TracingClientHttpInterceptor(String label, BodyMasker bodyMasker) {
    this.label = label;
    this.bodyMasker = bodyMasker;
  }

  @Override
  @NonNull
  public ClientHttpResponse intercept(
      HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution)
      throws IOException {

    String url = request.getURI().toString();
    String requestBody = bodyMasker.mask(new String(body, StandardCharsets.UTF_8));

    log.info("{} request START {} | Body: {}", label, url, requestBody);

    long start = System.nanoTime();
    try {
      ClientHttpResponse response = execution.execute(request, body);
      long durationMs = (System.nanoTime() - start) / 1_000_000;
      MDC.put(MDC_EXTERNAL_API_DURATION, String.valueOf(durationMs));

      byte[] responseBodyBytes = response.getBody().readAllBytes();
      String responseBody = bodyMasker.mask(new String(responseBodyBytes, StandardCharsets.UTF_8));
      log.info(
          "{} request END {} | {} | Response: {} | Time: {}ms",
          label,
          url,
          response.getStatusCode().value(),
          responseBody,
          durationMs);

      return new BufferedClientHttpResponse(response, responseBodyBytes);
    } catch (IOException e) {
      long durationMs = (System.nanoTime() - start) / 1_000_000;
      MDC.put(MDC_EXTERNAL_API_DURATION, String.valueOf(durationMs));
      log.error(
          "{} request FAILED {} | {} | Time: {}ms", label, url, e.getMessage(), durationMs, e);
      throw e;
    } finally {
      MDC.remove(MDC_EXTERNAL_API_DURATION);
    }
  }

  private static class BufferedClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse original;
    private final byte[] body;

    BufferedClientHttpResponse(ClientHttpResponse original, byte[] body) {
      this.original = original;
      this.body = body;
    }

    @Override
    @NonNull
    public HttpStatusCode getStatusCode() throws IOException {
      return original.getStatusCode();
    }

    @Override
    @NonNull
    public String getStatusText() throws IOException {
      return original.getStatusText();
    }

    @Override
    public void close() {
      original.close();
    }

    @Override
    @NonNull
    public InputStream getBody() {
      return new ByteArrayInputStream(body);
    }

    @Override
    @NonNull
    public HttpHeaders getHeaders() {
      return original.getHeaders();
    }
  }
}
