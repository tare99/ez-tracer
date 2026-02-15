# EZ Tracer

Drop-in HTTP request/response logging for Spring Boot. Add the dependency, and all incoming and outgoing HTTP traffic gets logged automatically with request IDs and duration tracking.

## Setup

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.tare99</groupId>
  <artifactId>ez-tracer</artifactId>
  <version>1.0</version>
</dependency>
```

That's it. Incoming requests are logged automatically.

## Outgoing requests

Add the interceptor to your desired HTTP client:

```java
@Bean
public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new TracingClientHttpInterceptor("Payment API"));
    return restTemplate;
}
```

The label (e.g. `"Payment API"`) appears in the log output to identify which service the call went to.

## Configuration

```yaml
ez-tracer:
  enabled: true              # default: true
  mask-fields:               # JSON field names to mask in logs
    - password
    - token
    - secret
  mask-replacement: "***"    # default: ***
```

### Field masking

Any JSON field name listed in `mask-fields` will have its value replaced in logs. The actual request/response is never modified.

Before:
```
Request START POST /api/login | Body: {"username":"john","password":"s3cret"}
```

After:
```
Request START POST /api/login | Body: {"username":"john","password":"***"}
```

Masking is case-insensitive and handles string, number, boolean, and null values.

To apply masking to outgoing requests as well, inject the `BodyMasker` bean. The fields are going to be injected from your application.yaml/properties, or you can create your own custom one for each HTTP client.

```java
@Bean
public RestTemplate restTemplate(BodyMasker ezTracerBodyMasker) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(
        new TracingClientHttpInterceptor("Payment API", ezTracerBodyMasker));
    return restTemplate;
}
```

## Log output

Each request produces two log lines with MDC context (`request_id`, `request_duration`):

```
INFO  [request_id=abc-123] Request START POST /api/login | Body: {"username":"john","password":"***"}
INFO  [request_id=abc-123, request_duration=45] Request END POST /api/login | Status: 200 | Body: {"token":"***"} | Time: 45ms
```

Paths containing `actuator`, `swagger`, `open-api`, or `api-docs` are automatically excluded.
