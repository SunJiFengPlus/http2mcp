package org.apache.camel.examples.openapi;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OpenApiHttpExecutor {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    public OpenApiHttpExecutor(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(OpenApiToolDescriptor descriptor, Map<String, Object> arguments) {
        URI uri = buildUri(descriptor, arguments);
        WebClient.RequestBodySpec requestSpec = this.webClient
            .method(descriptor.method())
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON);

        descriptor.defaultHeaders().forEach(requestSpec::header);
        applyHeaderParameters(descriptor.headerParameters(), arguments, requestSpec);

        Object body = descriptor.requestBodySchema() != null ? arguments.get("body") : null;
        ResponseEntity<String> response = send(descriptor.method(), requestSpec, body);
        return toToolResponse(uri, response);
    }

    private ResponseEntity<String> send(HttpMethod method, WebClient.RequestBodySpec requestSpec, Object body) {
        try {
            if (body != null && method != HttpMethod.GET && method != HttpMethod.DELETE) {
                return requestSpec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(String.class)
                    .block(DEFAULT_TIMEOUT);
            }
            return requestSpec.retrieve().toEntity(String.class).block(DEFAULT_TIMEOUT);
        }
        catch (WebClientResponseException ex) {
            throw new IllegalStateException("HTTP call failed: %s".formatted(ex.getResponseBodyAsString()), ex);
        }
    }

    private URI buildUri(OpenApiToolDescriptor descriptor, Map<String, Object> arguments) {
        Map<String, Object> pathVariables = descriptor.pathParameters()
            .stream()
            .collect(LinkedHashMap::new,
                    (map, parameter) -> map.put(parameter.name(), resolveRequiredValue(parameter, arguments)),
                    LinkedHashMap::putAll);

        UriComponentsBuilder baseBuilder = UriComponentsBuilder.fromHttpUrl(descriptor.serverUrl())
            .path(descriptor.pathTemplate());
        String resolvedPath = baseBuilder.buildAndExpand(pathVariables).toUriString();
        UriComponentsBuilder finalBuilder = UriComponentsBuilder.fromUriString(resolvedPath);
        descriptor.queryParameters()
            .forEach(parameter -> resolveOptionalValue(parameter, arguments)
                .ifPresent(value -> finalBuilder.queryParam(parameter.name(), value)));
        return finalBuilder.build(true).toUri();
    }

    private void applyHeaderParameters(List<OpenApiToolDescriptor.OpenApiParameter> headerParameters,
            Map<String, Object> arguments, WebClient.RequestBodySpec requestSpec) {
        headerParameters.forEach(parameter -> applyHeaderParameter(parameter, arguments, requestSpec));
    }

    private Object resolveRequiredValue(OpenApiToolDescriptor.OpenApiParameter parameter, Map<String, Object> arguments) {
        Object value = arguments.get(parameter.name());
        Assert.state(value != null, () -> "Missing required parameter: " + parameter.name());
        return value;
    }

    private Optional<Object> resolveOptionalValue(OpenApiToolDescriptor.OpenApiParameter parameter,
            Map<String, Object> arguments) {
        Object value = arguments.get(parameter.name());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private Map<String, Object> toToolResponse(URI uri, ResponseEntity<String> response) {
        Assert.notNull(response, "HTTP response cannot be null");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", uri.toString());
        payload.put("status", response.getStatusCode().value());
        payload.put("headers", flattenHeaders(response.getHeaders()));
        payload.put("body", decodeBody(response.getBody()));
        return payload;
    }

    private Map<String, String> flattenHeaders(MultiValueMap<String, String> headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.forEach((name, values) -> flattened.put(name, values.isEmpty() ? "" : values.get(0)));
        return flattened;
    }

    private Object decodeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            return this.objectMapper.readValue(body, Map.class);
        }
        catch (JsonProcessingException ex) {
            return body;
        }
    }

    private void applyHeaderParameter(OpenApiToolDescriptor.OpenApiParameter parameter, Map<String, Object> arguments,
            WebClient.RequestBodySpec requestSpec) {
        Optional<Object> value = resolveOptionalValue(parameter, arguments);
        if (value.isPresent()) {
            requestSpec.header(parameter.name(), value.get().toString());
            return;
        }
        Assert.state(!parameter.required(), () -> "Missing required header parameter: " + parameter.name());
    }
}
