package org.apache.camel.examples.openapi;

import java.util.Collections;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public record OpenApiDocument(
        String id,
        OpenAPI openApi,
        Map<String, String> defaultHeaders,
        String serverUrlOverride) {

    public OpenApiDocument {
        defaultHeaders = defaultHeaders != null ? Map.copyOf(defaultHeaders) : Collections.emptyMap();
    }
}
