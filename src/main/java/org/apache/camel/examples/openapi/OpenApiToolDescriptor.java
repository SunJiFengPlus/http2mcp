package org.apache.camel.examples.openapi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.media.Schema;

import org.springframework.http.HttpMethod;

public record OpenApiToolDescriptor(
        String specId,
        String toolName,
        String description,
        HttpMethod method,
        String serverUrl,
        String pathTemplate,
        List<OpenApiParameter> pathParameters,
        List<OpenApiParameter> queryParameters,
        List<OpenApiParameter> headerParameters,
        Schema<?> requestBodySchema,
        String requestBodyMediaType,
        boolean requestBodyRequired,
        Map<String, String> defaultHeaders) {

    public OpenApiToolDescriptor {
        pathParameters = pathParameters != null ? List.copyOf(pathParameters) : Collections.emptyList();
        queryParameters = queryParameters != null ? List.copyOf(queryParameters) : Collections.emptyList();
        headerParameters = headerParameters != null ? List.copyOf(headerParameters) : Collections.emptyList();
        defaultHeaders = defaultHeaders != null ? Map.copyOf(defaultHeaders) : Collections.emptyMap();
    }

    public record OpenApiParameter(String name, Schema<?> schema, boolean required, String description) {
    }
}
