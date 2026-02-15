package io.github.tare99.eztracer.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "ez-tracer")
public class TraceLoggerProperties {
  private Boolean enabled = true;
  private List<String> maskFields = List.of();
  private String maskReplacement = "***";
}
