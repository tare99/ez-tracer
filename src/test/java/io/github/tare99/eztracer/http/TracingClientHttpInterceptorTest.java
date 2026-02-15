package io.github.tare99.eztracer.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tare99.eztracer.mask.BodyMasker;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

class TracingClientHttpInterceptorTest {

  @Test
  void responseBodyIsReadableAfterInterception() throws IOException {
    BodyMasker masker = new BodyMasker(List.of("secret"), "***");
    TracingClientHttpInterceptor interceptor = new TracingClientHttpInterceptor("Test API", masker);

    HttpRequest request = mockRequest("https://api.example.com/data");
    byte[] requestBody = "{\"secret\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    String responseJson = "{\"secret\":\"response-secret\",\"data\":\"ok\"}";

    ClientHttpRequestExecution execution = (req, body) -> mockResponse(HttpStatus.OK, responseJson);

    ClientHttpResponse response = interceptor.intercept(request, requestBody, execution);

    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(body).isEqualTo(responseJson);
  }

  @Test
  void responseStatusIsPreserved() throws IOException {
    TracingClientHttpInterceptor interceptor = new TracingClientHttpInterceptor("Test API");

    HttpRequest request = mockRequest("https://api.example.com/data");

    ClientHttpRequestExecution execution = (req, body) -> mockResponse(HttpStatus.NOT_FOUND, "{}");

    ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void worksWithoutBodyMasker() throws IOException {
    TracingClientHttpInterceptor interceptor = new TracingClientHttpInterceptor("Test API");

    HttpRequest request = mockRequest("https://api.example.com/data");
    byte[] requestBody = "{\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8);

    ClientHttpRequestExecution execution =
        (req, body) -> mockResponse(HttpStatus.OK, "{\"token\":\"abc\"}");

    ClientHttpResponse response = interceptor.intercept(request, requestBody, execution);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void ioExceptionIsRethrown() {
    TracingClientHttpInterceptor interceptor = new TracingClientHttpInterceptor();

    HttpRequest request = mockRequest("https://api.example.com/fail");

    ClientHttpRequestExecution execution =
        (req, body) -> {
          throw new IOException("connection refused");
        };

    assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
        .isInstanceOf(IOException.class)
        .hasMessage("connection refused");
  }

  @Test
  void defaultLabelIsThirdParty() throws IOException {
    TracingClientHttpInterceptor interceptor = new TracingClientHttpInterceptor();

    HttpRequest request = mockRequest("https://api.example.com/data");
    ClientHttpRequestExecution execution = (req, body) -> mockResponse(HttpStatus.OK, "{}");

    ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private HttpRequest mockRequest(String url) {
    return new HttpRequest() {
      @Override
      public HttpMethod getMethod() {
        return HttpMethod.POST;
      }

      @Override
      public URI getURI() {
        return URI.create(url);
      }

      @Override
      public HttpHeaders getHeaders() {
        return new HttpHeaders();
      }
    };
  }

  private ClientHttpResponse mockResponse(HttpStatus status, String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return new MockClientHttpResponse(bytes, status);
  }
}
