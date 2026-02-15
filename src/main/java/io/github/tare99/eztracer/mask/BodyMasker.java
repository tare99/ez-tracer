package io.github.tare99.eztracer.mask;

import java.util.List;
import java.util.regex.Pattern;

public class BodyMasker {

  private final List<Pattern> patterns;
  private final String replacement;

  public BodyMasker(List<String> fieldNames, String replacement) {
    this.replacement = replacement;
    this.patterns =
        fieldNames.stream()
            .map(
                field ->
                    Pattern.compile(
                        "(\""
                            + Pattern.quote(field)
                            + "\"\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|\\d+(?:\\.\\d+)?|true|false|null)",
                        Pattern.CASE_INSENSITIVE))
            .toList();
  }

  public String mask(String body) {
    if (body == null || body.isEmpty() || patterns.isEmpty()) {
      return body;
    }
    String masked = body;
    for (Pattern pattern : patterns) {
      masked = pattern.matcher(masked).replaceAll("$1\"" + replacement + "\"");
    }
    return masked;
  }
}
