package org.apache.camel.examples.openapi;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.media.Schema;

import org.springframework.stereotype.Component;

@Component
public class OpenApiToolSchemaBuilder {

    private final ObjectMapper objectMapper;

    public OpenApiToolSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSchema(OpenApiToolDescriptor descriptor) {
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode properties = root.putObject("properties");
        ArrayNode required = this.objectMapper.createArrayNode();

        addParameters(descriptor.pathParameters(), properties, required);
        addParameters(descriptor.queryParameters(), properties, required);
        addParameters(descriptor.headerParameters(), properties, required);
        addRequestBody(descriptor, properties, required);

        if (!required.isEmpty()) {
            root.set("required", required);
        }
        try {
            return this.objectMapper.writeValueAsString(root);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool input schema for %s".formatted(
                    descriptor.toolName()), ex);
        }
    }

    private void addParameters(List<OpenApiToolDescriptor.OpenApiParameter> parameters, ObjectNode properties,
            ArrayNode required) {
        parameters.forEach(parameter -> applyParameterNode(parameter, properties, required));
    }

    private void addRequestBody(OpenApiToolDescriptor descriptor, ObjectNode properties, ArrayNode required) {
        Schema<?> schema = descriptor.requestBodySchema();
        if (schema == null) {
            return;
        }
        properties.set("body", schemaNode(schema, "HTTP request payload"));
        if (descriptor.requestBodyRequired()) {
            required.add("body");
        }
    }

    private JsonNode schemaNode(Schema<?> schema, String description) {
        ObjectNode node = schema != null ? this.objectMapper.valueToTree(schema) : defaultStringSchema();
        if (description != null) {
            node.put("description", description);
        }
        if (!node.has("type")) {
            node.put("type", "string");
        }
        return node;
    }

    private ObjectNode defaultStringSchema() {
        ObjectNode fallback = this.objectMapper.createObjectNode();
        fallback.put("type", "string");
        return fallback;
    }

    private void applyParameterNode(OpenApiToolDescriptor.OpenApiParameter parameter, ObjectNode properties,
            ArrayNode required) {
        properties.set(parameter.name(), schemaNode(parameter.schema(), parameter.description()));
        if (parameter.required()) {
            required.add(parameter.name());
        }
    }
}
