package io.github.tare99.eztracer.config;

import io.github.tare99.eztracer.filter.HttpRequestTracingFilter;
import io.github.tare99.eztracer.mask.BodyMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(TraceLoggerProperties.class)
@ConditionalOnProperty(
    prefix = "ez-tracer",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TraceLoggerAutoConfiguration {

  @Bean
  public BodyMasker ezTracerBodyMasker(TraceLoggerProperties properties) {
    return new BodyMasker(properties.getMaskFields(), properties.getMaskReplacement());
  }

  @Bean
  public FilterRegistrationBean<HttpRequestTracingFilter> httpRequestTracingFilter(
      BodyMasker ezTracerBodyMasker) {
    FilterRegistrationBean<HttpRequestTracingFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new HttpRequestTracingFilter(ezTracerBodyMasker));
    reg.addUrlPatterns("/*");
    reg.setOrder(Integer.MIN_VALUE + 10);
    reg.setName("ezTracerHttpFilter");
    return reg;
  }
}
