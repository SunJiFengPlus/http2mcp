package org.apache.camel.examples.openapi;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import org.springframework.stereotype.Component;

@Component
public class OpenApiToolSchemaBuilder {

    private final ObjectMapper objectMapper;

    public OpenApiToolSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSchema(String toolName, List<Parameter> pathParameters, List<Parameter> queryParameters,
            List<Parameter> headerParameters, Schema<?> requestBodySchema, boolean requestBodyRequired) {
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode properties = root.putObject("properties");
        ArrayNode required = this.objectMapper.createArrayNode();

        addParameters(pathParameters, properties, required);
        addParameters(queryParameters, properties, required);
        addParameters(headerParameters, properties, required);
        addRequestBody(requestBodySchema, requestBodyRequired, properties, required);

        if (!required.isEmpty()) {
            root.set("required", required);
        }
        try {
            return this.objectMapper.writeValueAsString(root);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialize tool input schema for %s".formatted(toolName), ex);
        }
    }

    private void addParameters(List<Parameter> parameters, ObjectNode properties, ArrayNode required) {
        parameters.forEach(parameter -> applyParameterNode(parameter, properties, required));
    }

    private void addRequestBody(Schema<?> schema, boolean requestBodyRequired, ObjectNode properties,
            ArrayNode required) {
        if (schema == null) {
            return;
        }
        properties.set("body", schemaNode(schema, "HTTP request payload"));
        if (requestBodyRequired) {
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

    private void applyParameterNode(Parameter parameter, ObjectNode properties, ArrayNode required) {
        properties.set(parameter.getName(), schemaNode(parameter.getSchema(), parameter.getDescription()));
        if (Boolean.TRUE.equals(parameter.getRequired())) {
            required.add(parameter.getName());
        }
    }
}
