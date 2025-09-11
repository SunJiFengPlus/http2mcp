package org.apache.camel.examples.openapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.apache.camel.examples.openapi.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于OpenAPI配置的动态MCP服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiMcpService {
    
    private final ProducerTemplate producerTemplate;
    private OpenApiConfig openApiConfig;
    private String baseUrl;
    
    /**
     * 设置OpenAPI配置
     */
    public void setOpenApiConfig(OpenApiConfig config) {
        this.openApiConfig = config;
        this.baseUrl = extractBaseUrl(config);
        log.info("已设置OpenAPI配置, 基础URL: {}", baseUrl);
    }
    
    /**
     * 动态生成并执行工具调用
     */
    public Object executeOpenApiTool(String operationId, Map<String, Object> parameters) {
        if (openApiConfig == null) {
            throw new IllegalStateException("OpenAPI配置未设置");
        }
        
        OperationInfo operationInfo = findOperationByOperationId(operationId)
            .orElseThrow(() -> new IllegalArgumentException("未找到操作: " + operationId));
        
        return executeOperation(operationInfo, parameters);
    }
    
    /**
     * 获取所有可用的工具信息
     */
    public List<ToolInfo> getAvailableTools() {
        if (openApiConfig == null || openApiConfig.getPaths() == null) {
            return Collections.emptyList();
        }
        
        return openApiConfig.getPaths().entrySet().stream()
            .flatMap(pathEntry -> extractOperationsFromPath(pathEntry.getKey(), pathEntry.getValue()).stream())
            .map(ToolInfo::new)
            .collect(Collectors.toList());
    }
    
    private String extractBaseUrl(OpenApiConfig config) {
        return Optional.ofNullable(config.getServers())
            .filter(servers -> !servers.isEmpty())
            .map(servers -> servers.get(0).getUrl())
            .orElse("");
    }
    
    private Optional<OperationInfo> findOperationByOperationId(String operationId) {
        if (openApiConfig == null || openApiConfig.getPaths() == null) {
            return Optional.empty();
        }
        
        return openApiConfig.getPaths().entrySet().stream()
            .flatMap(pathEntry -> extractOperationsFromPath(pathEntry.getKey(), pathEntry.getValue()).stream())
            .filter(op -> operationId.equals(op.getOperationId()))
            .findFirst();
    }
    
    private List<OperationInfo> extractOperationsFromPath(String path, PathItemConfig pathItem) {
        List<OperationInfo> operations = new ArrayList<>();
        
        Optional.ofNullable(pathItem.getGet())
            .ifPresent(op -> operations.add(new OperationInfo(path, "GET", op)));
            
        Optional.ofNullable(pathItem.getPost())
            .ifPresent(op -> operations.add(new OperationInfo(path, "POST", op)));
            
        Optional.ofNullable(pathItem.getPut())
            .ifPresent(op -> operations.add(new OperationInfo(path, "PUT", op)));
            
        Optional.ofNullable(pathItem.getDelete())
            .ifPresent(op -> operations.add(new OperationInfo(path, "DELETE", op)));
            
        Optional.ofNullable(pathItem.getPatch())
            .ifPresent(op -> operations.add(new OperationInfo(path, "PATCH", op)));
        
        return operations;
    }
    
    private Object executeOperation(OperationInfo operationInfo, Map<String, Object> parameters) {
        try {
            String fullUrl = buildFullUrl(operationInfo.getPath(), parameters);
            Map<String, String> headers = extractHeaders(operationInfo.getOperation(), parameters);
            Map<String, String> queryParams = extractQueryParams(operationInfo.getOperation(), parameters);
            String body = extractRequestBody(operationInfo.getOperation(), parameters);
            
            HttpRequestBean request = new HttpRequestBean(
                operationInfo.getMethod(),
                fullUrl,
                headers,
                body,
                queryParams
            );
            
            log.info("执行OpenAPI操作: {} {} {}", operationInfo.getMethod(), fullUrl, operationInfo.getOperationId());
            
            HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
            
            return processResponse(response, operationInfo.getOperation());
            
        } catch (Exception e) {
            log.error("执行OpenAPI操作失败: {}", operationInfo.getOperationId(), e);
            return Map.of(
                "error", true,
                "message", e.getMessage(),
                "operationId", operationInfo.getOperationId()
            );
        }
    }
    
    private String buildFullUrl(String path, Map<String, Object> parameters) {
        String processedPath = path;
        
        // 处理路径参数
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            String paramName = param.getKey();
            String paramValue = String.valueOf(param.getValue());
            
            // 替换路径中的参数占位符
            String placeholder = "{" + paramName + "}";
            if (processedPath.contains(placeholder)) {
                processedPath = processedPath.replace(placeholder, paramValue);
            }
        }
        
        return baseUrl + processedPath;
    }
    
    private Map<String, String> extractHeaders(OperationConfig operation, Map<String, Object> parameters) {
        Map<String, String> headers = new HashMap<>();
        
        Optional.ofNullable(operation.getParameters())
            .orElse(Collections.emptyList())
            .stream()
            .filter(param -> "header".equals(param.getIn()))
            .forEach(param -> {
                Object value = parameters.get(param.getName());
                if (value != null) {
                    headers.put(param.getName(), String.valueOf(value));
                }
            });
        
        return headers;
    }
    
    private Map<String, String> extractQueryParams(OperationConfig operation, Map<String, Object> parameters) {
        Map<String, String> queryParams = new HashMap<>();
        
        Optional.ofNullable(operation.getParameters())
            .orElse(Collections.emptyList())
            .stream()
            .filter(param -> "query".equals(param.getIn()))
            .forEach(param -> {
                Object value = parameters.get(param.getName());
                if (value != null) {
                    queryParams.put(param.getName(), String.valueOf(value));
                }
            });
        
        return queryParams;
    }
    
    private String extractRequestBody(OperationConfig operation, Map<String, Object> parameters) {
        if (operation.getRequestBody() == null) {
            return null;
        }
        
        // 简化处理：查找body参数或合并所有非路径、非查询、非头参数
        Object bodyParam = parameters.get("body");
        if (bodyParam != null) {
            return bodyParam instanceof String ? (String) bodyParam : bodyParam.toString();
        }
        
        // 收集所有不属于路径/查询/头的参数作为请求体
        Set<String> usedParams = Optional.ofNullable(operation.getParameters())
            .orElse(Collections.emptyList())
            .stream()
            .map(ParameterConfig::getName)
            .collect(Collectors.toSet());
        
        Map<String, Object> bodyParams = parameters.entrySet().stream()
            .filter(entry -> !usedParams.contains(entry.getKey()))
            .filter(entry -> !"body".equals(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        return bodyParams.isEmpty() ? null : bodyParams.toString();
    }
    
    private Object processResponse(HttpResponseBean response, OperationConfig operation) {
        // 基础响应处理
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCode());
        result.put("headers", response.getHeaders());
        
        try {
            // 尝试解析JSON响应
            String body = response.getBody();
            if (body != null && !body.trim().isEmpty()) {
                if (body.trim().startsWith("{") || body.trim().startsWith("[")) {
                    // JSON响应，直接返回
                    result.put("data", body);
                } else {
                    // 文本响应
                    result.put("data", body);
                }
            }
        } catch (Exception e) {
            log.warn("解析响应体失败，返回原始内容", e);
            result.put("data", response.getBody());
        }
        
        return result;
    }
    
    /**
     * 操作信息内部类
     */
    private static class OperationInfo {
        private final String path;
        private final String method;
        private final OperationConfig operation;
        
        public OperationInfo(String path, String method, OperationConfig operation) {
            this.path = path;
            this.method = method;
            this.operation = operation;
        }
        
        public String getPath() { return path; }
        public String getMethod() { return method; }
        public OperationConfig getOperation() { return operation; }
        public String getOperationId() { return operation.getOperationId(); }
    }
    
    /**
     * 工具信息
     */
    public static class ToolInfo {
        private final String operationId;
        private final String method;
        private final String path;
        private final String description;
        private final List<ParameterInfo> parameters;
        
        public ToolInfo(OperationInfo operationInfo) {
            this.operationId = operationInfo.getOperationId();
            this.method = operationInfo.getMethod();
            this.path = operationInfo.getPath();
            this.description = Optional.ofNullable(operationInfo.getOperation().getDescription())
                .orElse(operationInfo.getOperation().getSummary());
            this.parameters = extractParameterInfo(operationInfo.getOperation());
        }
        
        private List<ParameterInfo> extractParameterInfo(OperationConfig operation) {
            return Optional.ofNullable(operation.getParameters())
                .orElse(Collections.emptyList())
                .stream()
                .map(param -> new ParameterInfo(
                    param.getName(),
                    param.getDescription(),
                    param.isRequired(),
                    param.getIn(),
                    Optional.ofNullable(param.getSchema())
                        .map(SchemaConfig::getType)
                        .orElse("string")
                ))
                .collect(Collectors.toList());
        }
        
        // Getters
        public String getOperationId() { return operationId; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getDescription() { return description; }
        public List<ParameterInfo> getParameters() { return parameters; }
    }
    
    /**
     * 参数信息
     */
    public static class ParameterInfo {
        private final String name;
        private final String description;
        private final boolean required;
        private final String location;
        private final String type;
        
        public ParameterInfo(String name, String description, boolean required, String location, String type) {
            this.name = name;
            this.description = description;
            this.required = required;
            this.location = location;
            this.type = type;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
        public String getLocation() { return location; }
        public String getType() { return type; }
    }
}