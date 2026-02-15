package io.github.tare99.eztracer.mask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BodyMaskerTest {

  @Test
  void maskStringValue() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String result = masker.mask("{\"username\":\"john\",\"password\":\"s3cret\"}");

    assertThat(result).isEqualTo("{\"username\":\"john\",\"password\":\"***\"}");
  }

  @Test
  void maskNumericValue() {
    BodyMasker masker = new BodyMasker(List.of("pin"), "***");

    String result = masker.mask("{\"user\":\"john\",\"pin\":1234}");

    assertThat(result).isEqualTo("{\"user\":\"john\",\"pin\":\"***\"}");
  }

  @Test
  void maskBooleanValue() {
    BodyMasker masker = new BodyMasker(List.of("active"), "***");

    String result = masker.mask("{\"user\":\"john\",\"active\":true}");

    assertThat(result).isEqualTo("{\"user\":\"john\",\"active\":\"***\"}");
  }

  @Test
  void maskNullValue() {
    BodyMasker masker = new BodyMasker(List.of("token"), "***");

    String result = masker.mask("{\"token\":null}");

    assertThat(result).isEqualTo("{\"token\":\"***\"}");
  }

  @Test
  void maskMultipleFields() {
    BodyMasker masker = new BodyMasker(List.of("password", "token"), "***");

    String result = masker.mask("{\"password\":\"secret\",\"name\":\"john\",\"token\":\"abc123\"}");

    assertThat(result).isEqualTo("{\"password\":\"***\",\"name\":\"john\",\"token\":\"***\"}");
  }

  @Test
  void maskMultipleOccurrencesOfSameField() {
    BodyMasker masker = new BodyMasker(List.of("secret"), "***");

    String result = masker.mask("[{\"secret\":\"one\"},{\"secret\":\"two\"}]");

    assertThat(result).isEqualTo("[{\"secret\":\"***\"},{\"secret\":\"***\"}]");
  }

  @Test
  void maskIsCaseInsensitive() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String result = masker.mask("{\"Password\":\"secret\"}");

    assertThat(result).isEqualTo("{\"Password\":\"***\"}");
  }

  @Test
  void maskWithCustomReplacement() {
    BodyMasker masker = new BodyMasker(List.of("password"), "[REDACTED]");

    String result = masker.mask("{\"password\":\"secret\"}");

    assertThat(result).isEqualTo("{\"password\":\"[REDACTED]\"}");
  }

  @Test
  void maskHandlesEscapedQuotesInValue() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String result = masker.mask("{\"password\":\"pass\\\"word\"}");

    assertThat(result).isEqualTo("{\"password\":\"***\"}");
  }

  @Test
  void maskWithWhitespaceBetweenKeyAndValue() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String result = masker.mask("{\"password\" : \"secret\"}");

    assertThat(result).isEqualTo("{\"password\" : \"***\"}");
  }

  @Test
  void noMaskingWhenFieldNotPresent() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String body = "{\"username\":\"john\"}";
    String result = masker.mask(body);

    assertThat(result).isEqualTo(body);
  }

  @Test
  void noMaskingWhenFieldListIsEmpty() {
    BodyMasker masker = new BodyMasker(List.of(), "***");

    String body = "{\"password\":\"secret\"}";
    String result = masker.mask(body);

    assertThat(result).isEqualTo(body);
  }

  @Test
  void returnsNullForNullInput() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    assertThat(masker.mask(null)).isNull();
  }

  @Test
  void returnsEmptyForEmptyInput() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    assertThat(masker.mask("")).isEmpty();
  }

  @Test
  void nonJsonBodyIsReturnedUnchanged() {
    BodyMasker masker = new BodyMasker(List.of("password"), "***");

    String body = "plain text body with no json";
    assertThat(masker.mask(body)).isEqualTo(body);
  }

  @Test
  void maskDecimalNumericValue() {
    BodyMasker masker = new BodyMasker(List.of("balance"), "***");

    String result = masker.mask("{\"balance\":99.95}");

    assertThat(result).isEqualTo("{\"balance\":\"***\"}");
  }
}
