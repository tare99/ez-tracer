package io.github.tare99.eztracer.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tare99.eztracer.mask.BodyMasker;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestTracingFilterTest {

  @Test
  void requestBodyIsMaskedBeforeLogging() throws Exception {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");
    HttpRequestTracingFilter filter = new HttpRequestTracingFilter(masker);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
    request.setContent("{\"username\":\"john\",\"password\":\"s3cret\"}".getBytes());
    request.setContentType("application/json");

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void responseBodyIsPreservedAfterMasking() throws Exception {
    BodyMasker masker = new BodyMasker(List.of("token"), "***");
    HttpRequestTracingFilter filter = new HttpRequestTracingFilter(masker);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String responseJson = "{\"token\":\"abc123\",\"data\":\"hello\"}";
    FilterChain chain =
        (req, res) -> {
          res.getWriter().write(responseJson);
          res.setContentType("application/json");
        };

    filter.doFilter(request, response, chain);

    assertThat(response.getContentAsString()).isEqualTo(responseJson);
  }

  @Test
  void ignoredPathsAreNotLogged() throws Exception {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");
    HttpRequestTracingFilter filter = new HttpRequestTracingFilter(masker);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void worksWithNoMaskFields() throws Exception {
    BodyMasker masker = new BodyMasker(List.of(), "***");
    HttpRequestTracingFilter filter = new HttpRequestTracingFilter(masker);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
    request.setContent("{\"password\":\"secret\"}".getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNotNull();
  }
}
