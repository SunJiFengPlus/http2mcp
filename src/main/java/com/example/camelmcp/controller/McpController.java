package com.example.camelmcp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class McpController {

    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;

    public McpController(ProducerTemplate producerTemplate, ObjectMapper objectMapper) {
        this.producerTemplate = producerTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${mcp.defaultTransformScript:classpath:scripts/transform.groovy}")
    private String defaultTransformScript;

    @GetMapping(path = "/mcp/ndjson", produces = "application/x-ndjson")
    public Flux<String> ndjson(@RequestParam("url") String legacyUrl,
                               @RequestParam(value = "script", required = false) String scriptResource,
                               @RequestHeader Map<String, String> headers) {
        List<Map<String, Object>> events = requestEvents(legacyUrl, scriptResource, headers);
        return Flux.fromIterable(events)
                .map(this::toJsonLine)
                .concatWithValues("\n");
    }

    @GetMapping(path = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sse(@RequestParam("url") String legacyUrl,
                                             @RequestParam(value = "script", required = false) String scriptResource,
                                             @RequestHeader Map<String, String> headers) {
        List<Map<String, Object>> events = requestEvents(legacyUrl, scriptResource, headers);
        return Flux.fromIterable(events)
                .delayElements(Duration.ofMillis(10))
                .map(event -> ServerSentEvent.<String>builder()
                        .event(String.valueOf(event.getOrDefault("event", "message")))
                        .id(String.valueOf(event.getOrDefault("id", "")))
                        .data(toJsonSilently((Map<String, Object>) event.getOrDefault("data", Collections.emptyMap())))
                        .build());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> requestEvents(String legacyUrl, String scriptResource, Map<String, String> incomingHeaders) {
        String script = (scriptResource == null || scriptResource.isBlank()) ? defaultTransformScript : scriptResource;
        Map<String, Object> headers = new java.util.HashMap<>();
        headers.putAll(incomingHeaders);
        headers.put("targetUrl", legacyUrl);
        headers.put("transformScriptResource", script);
        Object result = producerTemplate.requestBodyAndHeaders("direct:transform", null, headers);
        if (result instanceof List<?>) {
            return (List<Map<String, Object>>) result;
        }
        if (result instanceof Map<?, ?>) {
            return List.of((Map<String, Object>) result);
        }
        return List.of(Map.of("event", "message", "data", Map.of("text", String.valueOf(result))));
    }

    private String toJsonLine(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event) + "\n";
        } catch (JsonProcessingException e) {
            return "{\"event\":\"error\",\"data\":{\"message\":\"json_error\"}}\n";
        }
    }

    private String toJsonSilently(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

