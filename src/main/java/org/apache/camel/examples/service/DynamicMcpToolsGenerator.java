package org.apache.camel.examples.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态MCP工具生成器
 * 根据OpenAPI规范动态生成和执行MCP工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpToolsGenerator {

    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;
    
    // 存储OpenAPI规范和工具定义
    private OpenAPI currentOpenAPI;
    private final Map<String, DynamicToolDefinition> toolDefinitions = new HashMap<>();

    /**
     * 动态工具定义
     */
    public static class DynamicToolDefinition {
        public final String operationId;
        public final String method;
        public final String path;
        public final String baseUrl;
        public final String summary;
        public final String description;
        public final List<DynamicParameter> parameters;
        public final Schema<?> requestBodySchema;
        public final boolean hasRequestBody;

        public DynamicToolDefinition(String operationId, String method, String path, String baseUrl,
                                   String summary, String description, List<DynamicParameter> parameters,
                                   Schema<?> requestBodySchema, boolean hasRequestBody) {
            this.operationId = operationId;
            this.method = method;
            this.path = path;
            this.baseUrl = baseUrl;
            this.summary = summary;
            this.description = description;
            this.parameters = parameters;
            this.requestBodySchema = requestBodySchema;
            this.hasRequestBody = hasRequestBody;
        }
    }

    /**
     * 动态参数定义
     */
    public static class DynamicParameter {
        public final String name;
        public final String type;
        public final String location; // query, path, header
        public final boolean required;
        public final String description;
        public final Schema<?> schema;

        public DynamicParameter(String name, String type, String location, boolean required,
                              String description, Schema<?> schema) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.required = required;
            this.description = description;
            this.schema = schema;
        }
    }

    /**
     * 加载OpenAPI规范并生成工具定义
     */
    public void loadOpenApiSpec(OpenAPI openAPI) {
        this.currentOpenAPI = openAPI;
        this.toolDefinitions.clear();
        
        if (openAPI.getPaths() == null) {
            log.warn("OpenAPI规范中没有定义路径");
            return;
        }

        String baseUrl = extractBaseUrl(openAPI);
        
        openAPI.getPaths().forEach((path, pathItem) -> {
            generateToolsForPath(path, pathItem, baseUrl);
        });

        log.info("成功生成 {} 个动态工具定义", toolDefinitions.size());
    }

    /**
     * 为路径生成工具定义
     */
    private void generateToolsForPath(String path, PathItem pathItem, String baseUrl) {
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
        
        operations.forEach((httpMethod, operation) -> {
            if (operation.getOperationId() != null) {
                DynamicToolDefinition toolDef = createToolDefinition(
                    httpMethod.name(), path, operation, baseUrl);
                toolDefinitions.put(operation.getOperationId(), toolDef);
                log.debug("生成工具定义: {} {} [{}]", httpMethod, path, operation.getOperationId());
            }
        });
    }

    /**
     * 创建工具定义
     */
    private DynamicToolDefinition createToolDefinition(String method, String path, Operation operation, String baseUrl) {
        String operationId = operation.getOperationId();
        String summary = operation.getSummary();
        String description = operation.getDescription();
        
        // 如果没有描述，使用摘要
        if (description == null || description.trim().isEmpty()) {
            description = summary != null ? summary : "从OpenAPI生成: " + method + " " + path;
        }

        List<DynamicParameter> parameters = extractDynamicParameters(operation);
        Schema<?> requestBodySchema = extractRequestBodySchema(operation);
        boolean hasRequestBody = operation.getRequestBody() != null;

        return new DynamicToolDefinition(operationId, method, path, baseUrl, summary, description,
                                       parameters, requestBodySchema, hasRequestBody);
    }

    /**
     * 提取动态参数
     */
    private List<DynamicParameter> extractDynamicParameters(Operation operation) {
        List<DynamicParameter> parameters = new ArrayList<>();
        
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                String type = extractParameterType(param);
                parameters.add(new DynamicParameter(
                    param.getName(),
                    type,
                    param.getIn(),
                    param.getRequired() != null && param.getRequired(),
                    param.getDescription() != null ? param.getDescription() : param.getName(),
                    param.getSchema()
                ));
            }
        }
        
        return parameters;
    }

    /**
     * 提取请求体Schema
     */
    private Schema<?> extractRequestBodySchema(Operation operation) {
        if (operation.getRequestBody() == null) {
            return null;
        }
        
        RequestBody requestBody = operation.getRequestBody();
        Content content = requestBody.getContent();
        
        if (content != null) {
            // 优先选择JSON内容
            MediaType jsonContent = content.get("application/json");
            if (jsonContent != null && jsonContent.getSchema() != null) {
                return jsonContent.getSchema();
            }
            
            // 如果没有JSON，选择第一个可用的内容类型
            for (MediaType mediaType : content.values()) {
                if (mediaType.getSchema() != null) {
                    return mediaType.getSchema();
                }
            }
        }
        
        return null;
    }

    /**
     * 提取参数类型
     */
    private String extractParameterType(Parameter param) {
        if (param.getSchema() == null) {
            return "string";
        }
        
        String type = param.getSchema().getType();
        if (type == null) {
            return "string";
        }
        
        // 将OpenAPI类型映射为Java类型描述
        switch (type.toLowerCase()) {
            case "integer":
                return "integer";
            case "number":
                return "number";
            case "boolean":
                return "boolean";
            case "array":
                return "array";
            case "object":
                return "object";
            default:
                return "string";
        }
    }

    /**
     * 提取基础URL
     */
    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            return server.getUrl();
        }
        return "http://localhost:8080";
    }

    /**
     * 通用工具执行方法
     * 这是一个通用的MCP工具，可以执行任何已注册的OpenAPI操作
     */
    @Tool(description = "执行OpenAPI操作的通用工具")
    public HttpResponseBean executeOpenApiOperation(
            @ToolParam(description = "操作ID，对应OpenAPI中的operationId") String operationId,
            @ToolParam(description = "查询参数，JSON字符串格式", required = false) String queryParams,
            @ToolParam(description = "路径参数，JSON字符串格式", required = false) String pathParams,
            @ToolParam(description = "请求头，JSON字符串格式", required = false) String headers,
            @ToolParam(description = "请求体，JSON字符串格式", required = false) String requestBody) {

        DynamicToolDefinition toolDef = toolDefinitions.get(operationId);
        if (toolDef == null) {
            return createErrorResponse(404, "操作未找到: " + operationId);
        }

        try {
            // 解析参数
            Map<String, String> queryParamsMap = parseJsonToStringMap(queryParams);
            Map<String, String> pathParamsMap = parseJsonToStringMap(pathParams);
            Map<String, String> headersMap = parseJsonToStringMap(headers);

            // 构建URL
            String url = buildUrlWithParams(toolDef.baseUrl, toolDef.path, pathParamsMap);

            // 创建HTTP请求
            HttpRequestBean httpRequest = new HttpRequestBean(
                toolDef.method,
                url,
                headersMap,
                requestBody,
                queryParamsMap
            );

            // 执行请求
            return producerTemplate.requestBody("direct:httpRequest", httpRequest, HttpResponseBean.class);
            
        } catch (Exception e) {
            log.error("执行OpenAPI操作失败: {}", operationId, e);
            return createErrorResponse(500, "执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的工具列表
     */
    @Tool(description = "获取所有可用的OpenAPI工具列表")
    public Map<String, String> listAvailableTools() {
        return toolDefinitions.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    DynamicToolDefinition def = entry.getValue();
                    return String.format("%s %s - %s", 
                        def.method, 
                        def.path, 
                        def.description != null ? def.description : "无描述");
                }
            ));
    }

    /**
     * 获取特定工具的详细信息
     */
    @Tool(description = "获取特定OpenAPI工具的详细信息")
    public Map<String, Object> getToolDetails(@ToolParam(description = "操作ID") String operationId) {
        DynamicToolDefinition toolDef = toolDefinitions.get(operationId);
        if (toolDef == null) {
            return Map.of("error", "操作未找到: " + operationId);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("operationId", toolDef.operationId);
        details.put("method", toolDef.method);
        details.put("path", toolDef.path);
        details.put("baseUrl", toolDef.baseUrl);
        details.put("description", toolDef.description);
        details.put("hasRequestBody", toolDef.hasRequestBody);
        
        List<Map<String, Object>> paramDetails = toolDef.parameters.stream()
            .map(param -> {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("name", param.name);
                paramMap.put("type", param.type);
                paramMap.put("location", param.location);
                paramMap.put("required", param.required);
                paramMap.put("description", param.description);
                return paramMap;
            })
            .collect(Collectors.toList());
        
        details.put("parameters", paramDetails);
        
        return details;
    }

    // === 私有辅助方法 ===

    /**
     * 解析JSON字符串为字符串Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonToStringMap(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
            return map.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().toString()
                ));
        } catch (JsonProcessingException e) {
            log.warn("解析JSON参数失败: {}", jsonString, e);
            return null;
        }
    }

    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String baseUrl, String path, Map<String, String> pathParams) {
        String fullUrl = combineUrls(baseUrl, path);
        
        if (pathParams != null && !pathParams.isEmpty()) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                fullUrl = fullUrl.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return fullUrl;
    }

    /**
     * 合并基础URL和路径
     */
    private String combineUrls(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    /**
     * 创建错误响应
     */
    private HttpResponseBean createErrorResponse(int statusCode, String message) {
        HttpResponseBean response = new HttpResponseBean();
        response.setStatusCode(statusCode);
        try {
            Map<String, String> errorMap = Map.of("error", message);
            response.setBody(objectMapper.writeValueAsString(errorMap));
        } catch (JsonProcessingException e) {
            response.setBody("{\"error\":\"" + message + "\"}");
        }
        return response;
    }

    /**
     * 获取当前加载的OpenAPI规范
     */
    public OpenAPI getCurrentOpenAPI() {
        return currentOpenAPI;
    }

    /**
     * 获取工具定义数量
     */
    public int getToolDefinitionsCount() {
        return toolDefinitions.size();
    }

    /**
     * 清空所有工具定义
     */
    public void clearToolDefinitions() {
        toolDefinitions.clear();
        currentOpenAPI = null;
        log.info("已清空所有动态工具定义");
    }
}