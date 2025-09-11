package org.apache.camel.examples.openapi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.examples.openapi.model.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * OpenAPI文件解析器
 */
@Slf4j
@Component
public class OpenApiParser {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * 从文件路径解析OpenAPI配置
     */
    public Optional<OpenApiConfig> parseFromFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                log.warn("OpenAPI文件不存在: {}", filePath);
                return Optional.empty();
            }
            
            String content = Files.readString(path);
            return parseFromContent(content, filePath.toLowerCase().endsWith(".yaml") || filePath.toLowerCase().endsWith(".yml"));
            
        } catch (IOException e) {
            log.error("读取OpenAPI文件失败: {}", filePath, e);
            return Optional.empty();
        }
    }
    
    /**
     * 从内容解析OpenAPI配置
     */
    public Optional<OpenApiConfig> parseFromContent(String content, boolean isYaml) {
        try {
            // 使用Swagger Parser进行预处理和验证
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content, null, null).getOpenAPI();
            if (openAPI == null) {
                log.warn("无法解析OpenAPI内容");
                return Optional.empty();
            }
            
            // 使用Jackson解析为我们的模型
            ObjectMapper mapper = isYaml ? yamlMapper : jsonMapper;
            JsonNode rootNode = mapper.readTree(content);
            
            return Optional.of(convertToOpenApiConfig(rootNode));
            
        } catch (Exception e) {
            log.error("解析OpenAPI内容失败", e);
            return Optional.empty();
        }
    }
    
    /**
     * 将JsonNode转换为OpenApiConfig
     */
    private OpenApiConfig convertToOpenApiConfig(JsonNode rootNode) {
        OpenApiConfig config = new OpenApiConfig();
        
        // 基本信息
        if (rootNode.has("openapi")) {
            config.setOpenapi(rootNode.get("openapi").asText());
        }
        
        // 信息配置
        if (rootNode.has("info")) {
            config.setInfo(convertToInfoConfig(rootNode.get("info")));
        }
        
        // 服务器配置
        if (rootNode.has("servers") && rootNode.get("servers").isArray()) {
            JsonNode serversNode = rootNode.get("servers");
            config.setServers(
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(serversNode.elements(), Spliterator.ORDERED), 
                    false
                )
                    .map(this::convertToServerConfig)
                    .collect(Collectors.toList())
            );
        }
        
        // 路径配置
        if (rootNode.has("paths")) {
            JsonNode pathsNode = rootNode.get("paths");
            Map<String, PathItemConfig> pathsMap = new HashMap<>();
            pathsNode.fieldNames().forEachRemaining(pathName -> {
                JsonNode pathNode = pathsNode.get(pathName);
                pathsMap.put(pathName, convertToPathItemConfig(pathNode));
            });
            config.setPaths(pathsMap);
        }
        
        // 组件配置
        if (rootNode.has("components")) {
            config.setComponents(convertToComponentsConfig(rootNode.get("components")));
        }
        
        return config;
    }
    
    private InfoConfig convertToInfoConfig(JsonNode infoNode) {
        InfoConfig info = new InfoConfig();
        
        Optional.ofNullable(infoNode.get("title"))
            .map(JsonNode::asText)
            .ifPresent(info::setTitle);
            
        Optional.ofNullable(infoNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(info::setDescription);
            
        Optional.ofNullable(infoNode.get("version"))
            .map(JsonNode::asText)
            .ifPresent(info::setVersion);
        
        return info;
    }
    
    private ServerConfig convertToServerConfig(JsonNode serverNode) {
        ServerConfig server = new ServerConfig();
        
        Optional.ofNullable(serverNode.get("url"))
            .map(JsonNode::asText)
            .ifPresent(server::setUrl);
            
        Optional.ofNullable(serverNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(server::setDescription);
        
        return server;
    }
    
    private PathItemConfig convertToPathItemConfig(JsonNode pathNode) {
        PathItemConfig pathItem = new PathItemConfig();
        
        Optional.ofNullable(pathNode.get("get"))
            .map(this::convertToOperationConfig)
            .ifPresent(pathItem::setGet);
            
        Optional.ofNullable(pathNode.get("post"))
            .map(this::convertToOperationConfig)
            .ifPresent(pathItem::setPost);
            
        Optional.ofNullable(pathNode.get("put"))
            .map(this::convertToOperationConfig)
            .ifPresent(pathItem::setPut);
            
        Optional.ofNullable(pathNode.get("delete"))
            .map(this::convertToOperationConfig)
            .ifPresent(pathItem::setDelete);
            
        Optional.ofNullable(pathNode.get("patch"))
            .map(this::convertToOperationConfig)
            .ifPresent(pathItem::setPatch);
        
        return pathItem;
    }
    
    private OperationConfig convertToOperationConfig(JsonNode operationNode) {
        OperationConfig operation = new OperationConfig();
        
        Optional.ofNullable(operationNode.get("operationId"))
            .map(JsonNode::asText)
            .ifPresent(operation::setOperationId);
            
        Optional.ofNullable(operationNode.get("summary"))
            .map(JsonNode::asText)
            .ifPresent(operation::setSummary);
            
        Optional.ofNullable(operationNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(operation::setDescription);
            
        // 标签
        if (operationNode.has("tags") && operationNode.get("tags").isArray()) {
            JsonNode tagsNode = operationNode.get("tags");
            operation.setTags(
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(tagsNode.elements(), Spliterator.ORDERED), 
                    false
                )
                    .map(JsonNode::asText)
                    .collect(Collectors.toList())
            );
        }
        
        // 参数
        if (operationNode.has("parameters") && operationNode.get("parameters").isArray()) {
            JsonNode parametersNode = operationNode.get("parameters");
            operation.setParameters(
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(parametersNode.elements(), Spliterator.ORDERED), 
                    false
                )
                    .map(this::convertToParameterConfig)
                    .collect(Collectors.toList())
            );
        }
        
        // 请求体
        Optional.ofNullable(operationNode.get("requestBody"))
            .map(this::convertToRequestBodyConfig)
            .ifPresent(operation::setRequestBody);
            
        // 响应
        if (operationNode.has("responses")) {
            JsonNode responsesNode = operationNode.get("responses");
            Map<String, ResponseConfig> responsesMap = new HashMap<>();
            responsesNode.fieldNames().forEachRemaining(statusCode -> {
                JsonNode responseNode = responsesNode.get(statusCode);
                responsesMap.put(statusCode, convertToResponseConfig(responseNode));
            });
            operation.setResponses(responsesMap);
        }
        
        return operation;
    }
    
    private ParameterConfig convertToParameterConfig(JsonNode parameterNode) {
        ParameterConfig parameter = new ParameterConfig();
        
        Optional.ofNullable(parameterNode.get("name"))
            .map(JsonNode::asText)
            .ifPresent(parameter::setName);
            
        Optional.ofNullable(parameterNode.get("in"))
            .map(JsonNode::asText)
            .ifPresent(parameter::setIn);
            
        Optional.ofNullable(parameterNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(parameter::setDescription);
            
        parameter.setRequired(
            Optional.ofNullable(parameterNode.get("required"))
                .map(JsonNode::asBoolean)
                .orElse(false)
        );
        
        Optional.ofNullable(parameterNode.get("schema"))
            .map(this::convertToSchemaConfig)
            .ifPresent(parameter::setSchema);
        
        return parameter;
    }
    
    private RequestBodyConfig convertToRequestBodyConfig(JsonNode requestBodyNode) {
        RequestBodyConfig requestBody = new RequestBodyConfig();
        
        Optional.ofNullable(requestBodyNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(requestBody::setDescription);
            
        requestBody.setRequired(
            Optional.ofNullable(requestBodyNode.get("required"))
                .map(JsonNode::asBoolean)
                .orElse(false)
        );
        
        if (requestBodyNode.has("content")) {
            JsonNode contentNode = requestBodyNode.get("content");
            Map<String, MediaTypeConfig> contentMap = new HashMap<>();
            contentNode.fieldNames().forEachRemaining(mediaType -> {
                JsonNode mediaTypeNode = contentNode.get(mediaType);
                contentMap.put(mediaType, convertToMediaTypeConfig(mediaTypeNode));
            });
            requestBody.setContent(contentMap);
        }
        
        return requestBody;
    }
    
    private ResponseConfig convertToResponseConfig(JsonNode responseNode) {
        ResponseConfig response = new ResponseConfig();
        
        Optional.ofNullable(responseNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(response::setDescription);
            
        if (responseNode.has("content")) {
            JsonNode contentNode = responseNode.get("content");
            Map<String, MediaTypeConfig> contentMap = new HashMap<>();
            contentNode.fieldNames().forEachRemaining(mediaType -> {
                JsonNode mediaTypeNode = contentNode.get(mediaType);
                contentMap.put(mediaType, convertToMediaTypeConfig(mediaTypeNode));
            });
            response.setContent(contentMap);
        }
        
        return response;
    }
    
    private MediaTypeConfig convertToMediaTypeConfig(JsonNode mediaTypeNode) {
        MediaTypeConfig mediaType = new MediaTypeConfig();
        
        Optional.ofNullable(mediaTypeNode.get("schema"))
            .map(this::convertToSchemaConfig)
            .ifPresent(mediaType::setSchema);
            
        Optional.ofNullable(mediaTypeNode.get("example"))
            .ifPresent(node -> mediaType.setExample(convertJsonNodeToObject(node)));
        
        return mediaType;
    }
    
    private SchemaConfig convertToSchemaConfig(JsonNode schemaNode) {
        SchemaConfig schema = new SchemaConfig();
        
        Optional.ofNullable(schemaNode.get("type"))
            .map(JsonNode::asText)
            .ifPresent(schema::setType);
            
        Optional.ofNullable(schemaNode.get("format"))
            .map(JsonNode::asText)
            .ifPresent(schema::setFormat);
            
        Optional.ofNullable(schemaNode.get("description"))
            .map(JsonNode::asText)
            .ifPresent(schema::setDescription);
            
        Optional.ofNullable(schemaNode.get("$ref"))
            .map(JsonNode::asText)
            .ifPresent(schema::setRef);
            
        if (schemaNode.has("enum") && schemaNode.get("enum").isArray()) {
            JsonNode enumNode = schemaNode.get("enum");
            schema.setEnumValues(
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(enumNode.elements(), Spliterator.ORDERED), 
                    false
                )
                    .map(this::convertJsonNodeToObject)
                    .collect(Collectors.toList())
            );
        }
        
        if (schemaNode.has("properties")) {
            JsonNode propertiesNode = schemaNode.get("properties");
            Map<String, SchemaConfig> propertiesMap = new HashMap<>();
            propertiesNode.fieldNames().forEachRemaining(propertyName -> {
                JsonNode propertyNode = propertiesNode.get(propertyName);
                propertiesMap.put(propertyName, convertToSchemaConfig(propertyNode));
            });
            schema.setProperties(propertiesMap);
        }
        
        if (schemaNode.has("required") && schemaNode.get("required").isArray()) {
            JsonNode requiredNode = schemaNode.get("required");
            schema.setRequired(
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(requiredNode.elements(), Spliterator.ORDERED), 
                    false
                )
                    .map(JsonNode::asText)
                    .collect(Collectors.toList())
            );
        }
        
        Optional.ofNullable(schemaNode.get("items"))
            .map(this::convertToSchemaConfig)
            .ifPresent(schema::setItems);
            
        Optional.ofNullable(schemaNode.get("default"))
            .ifPresent(node -> schema.setDefaultValue(convertJsonNodeToObject(node)));
        
        return schema;
    }
    
    private ComponentsConfig convertToComponentsConfig(JsonNode componentsNode) {
        ComponentsConfig components = new ComponentsConfig();
        
        if (componentsNode.has("schemas")) {
            JsonNode schemasNode = componentsNode.get("schemas");
            Map<String, SchemaConfig> schemasMap = new HashMap<>();
            schemasNode.fieldNames().forEachRemaining(schemaName -> {
                JsonNode schemaNode = schemasNode.get(schemaName);
                schemasMap.put(schemaName, convertToSchemaConfig(schemaNode));
            });
            components.setSchemas(schemasMap);
        }
        
        return components;
    }
    
    private Object convertJsonNodeToObject(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        }
        return node.toString();
    }
}