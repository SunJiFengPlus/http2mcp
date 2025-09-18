package org.apache.camel.examples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简化的JsonSchemaGenerator，参考Spring AI的实现方式
 */
public final class JsonSchemaGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSchemaGenerator() {
    }

    /**
     * 为OpenAPI操作生成JSON Schema
     */
    public static String generateForOpenApiOperation(Operation operation, String path) {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        // 提取路径参数
        extractPathParameters(operation, path, properties, required);

        // 处理操作参数
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(param -> param.getSchema() != null)
                .forEach(param -> {
                    String paramName = param.getName();
                    if (!properties.has(paramName)) {
                        properties.set(paramName, createSimpleSchema(param.getSchema(), param.getDescription()));
                        if (param.getRequired() != null && param.getRequired()) {
                            required.add(paramName);
                        }
                    }
                });
        }

        // 处理请求体
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            properties.set("body", createRequestBodySchema(requestBody));
            if (requestBody.getRequired() != null && requestBody.getRequired()) {
                required.add("body");
            }
        }

        return schema.toPrettyString();
    }

    private static void extractPathParameters(Operation operation, String path, ObjectNode properties, ArrayNode required) {
        Pattern pattern = Pattern.compile("\\{([^}]+)}");
        Matcher matcher = pattern.matcher(path);
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            ObjectNode paramSchema = OBJECT_MAPPER.createObjectNode();
            paramSchema.put("type", "string");
            
            // 尝试从操作参数中获取描述
            String description = "Path parameter: " + paramName; // 默认描述
            if (operation.getParameters() != null) {
                operation.getParameters().stream()
                    .filter(param -> param.getName().equals(paramName) && "path".equals(param.getIn()))
                    .findFirst()
                    .ifPresent(param -> {
                        if (param.getDescription() != null && !param.getDescription().trim().isEmpty()) {
                            paramSchema.put("description", param.getDescription());
                        } else {
                            paramSchema.put("description", description);
                        }
                    });
            } else {
                paramSchema.put("description", description);
            }
            
            properties.set(paramName, paramSchema);
            required.add(paramName);
        }
    }

    private static ObjectNode createSimpleSchema(Schema<?> schema, String description) {
        ObjectNode jsonSchema = OBJECT_MAPPER.createObjectNode();
        
        if (schema.getType() != null) {
            jsonSchema.put("type", schema.getType());
        }
        if (description != null && !description.trim().isEmpty()) {
            jsonSchema.put("description", description);
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            ArrayNode enumArray = jsonSchema.putArray("enum");
            schema.getEnum().forEach(enumValue -> enumArray.add(OBJECT_MAPPER.valueToTree(enumValue)));
        }

        return jsonSchema;
    }

    private static ObjectNode createRequestBodySchema(RequestBody requestBody) {
        Content content = requestBody.getContent();
        if (content != null && content.get("application/json") != null) {
            Schema<?> schema = content.get("application/json").getSchema();
            if (schema != null) {
                ObjectNode bodySchema = OBJECT_MAPPER.createObjectNode();
                bodySchema.put("type", "object");
                if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                    ObjectNode properties = bodySchema.putObject("properties");
                    schema.getProperties().forEach((name, propSchema) -> {
                        ObjectNode propJsonSchema = OBJECT_MAPPER.createObjectNode();
                        if (propSchema.getType() != null) {
                            propJsonSchema.put("type", propSchema.getType());
                        }
                        if (propSchema.getDescription() != null) {
                            propJsonSchema.put("description", propSchema.getDescription());
                        }
                        properties.set(name, propJsonSchema);
                    });
                }
                if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                    ArrayNode required = bodySchema.putArray("required");
                    schema.getRequired().forEach(required::add);
                }
                return bodySchema;
            }
        }
        
        // 默认返回简单的对象schema
        ObjectNode defaultSchema = OBJECT_MAPPER.createObjectNode();
        defaultSchema.put("type", "object");
        return defaultSchema;
    }
}
