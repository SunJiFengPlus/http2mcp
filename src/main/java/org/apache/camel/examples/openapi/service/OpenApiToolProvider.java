package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 简化的OpenAPI工具提供者
 * 直接使用@Tool注解而不是动态代理
 */
@Slf4j
public class OpenApiToolProvider {
    
    @Autowired
    ProducerTemplate producerTemplate;
    
    private final OpenAPI openAPI;
    private final String baseUrl;
    
    public OpenApiToolProvider(OpenAPI openAPI) {
        this.openAPI = openAPI;
        this.baseUrl = extractBaseUrl(openAPI);
    }
    
    /**
     * 通用的OpenAPI工具执行方法
     */
    @Tool(description = "执行OpenAPI定义的HTTP操作")
    public Object executeOpenApiOperation(
            @ToolParam(description = "操作ID，对应OpenAPI中的operationId") String operationId,
            @ToolParam(description = "请求参数，包含路径参数、查询参数、请求头和请求体", required = false) Map<String, Object> parameters
    ) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        log.info("执行OpenAPI操作: {}", operationId);
        
        try {
            // 查找对应的操作
            OperationInfo operationInfo = findOperationByOperationId(operationId);
            if (operationInfo == null) {
                return Map.of("error", true, "message", "未找到操作: " + operationId);
            }
            
            // 构建HTTP请求
            String fullUrl = buildFullUrl(operationInfo.path, parameters);
            Map<String, String> headers = extractHeaders(operationInfo.operation, parameters);
            Map<String, String> queryParams = extractQueryParams(operationInfo.operation, parameters);
            String body = extractRequestBody(operationInfo.operation, parameters);
            
            HttpRequestBean request = new HttpRequestBean(
                operationInfo.method,
                fullUrl,
                headers,
                body,
                queryParams
            );
            
            log.info("执行HTTP请求: {} {}", operationInfo.method, fullUrl);
            
            HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
            
            return processResponse(response);
            
        } catch (Exception e) {
            log.error("执行OpenAPI操作失败: {}", operationId, e);
            return Map.of(
                "error", true,
                "message", e.getMessage(),
                "operationId", operationId
            );
        }
    }
    
    private String extractBaseUrl(OpenAPI openAPI) {
        return Optional.ofNullable(openAPI.getServers())
            .filter(servers -> !servers.isEmpty())
            .map(servers -> servers.get(0).getUrl())
            .orElse("");
    }
    
    private OperationInfo findOperationByOperationId(String operationId) {
        if (openAPI.getPaths() == null) {
            return null;
        }
        
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();
            
            if (pathItem.getGet() != null && operationId.equals(pathItem.getGet().getOperationId())) {
                return new OperationInfo(path, "GET", pathItem.getGet());
            }
            if (pathItem.getPost() != null && operationId.equals(pathItem.getPost().getOperationId())) {
                return new OperationInfo(path, "POST", pathItem.getPost());
            }
            if (pathItem.getPut() != null && operationId.equals(pathItem.getPut().getOperationId())) {
                return new OperationInfo(path, "PUT", pathItem.getPut());
            }
            if (pathItem.getDelete() != null && operationId.equals(pathItem.getDelete().getOperationId())) {
                return new OperationInfo(path, "DELETE", pathItem.getDelete());
            }
            if (pathItem.getPatch() != null && operationId.equals(pathItem.getPatch().getOperationId())) {
                return new OperationInfo(path, "PATCH", pathItem.getPatch());
            }
        }
        
        return null;
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
    
    private Map<String, String> extractHeaders(Operation operation, Map<String, Object> parameters) {
        Map<String, String> headers = new HashMap<>();
        
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(param -> "header".equals(param.getIn()))
                .forEach(param -> {
                    Object value = parameters.get(param.getName());
                    if (value != null) {
                        headers.put(param.getName(), String.valueOf(value));
                    }
                });
        }
        
        return headers;
    }
    
    private Map<String, String> extractQueryParams(Operation operation, Map<String, Object> parameters) {
        Map<String, String> queryParams = new HashMap<>();
        
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(param -> "query".equals(param.getIn()))
                .forEach(param -> {
                    Object value = parameters.get(param.getName());
                    if (value != null) {
                        queryParams.put(param.getName(), String.valueOf(value));
                    }
                });
        }
        
        return queryParams;
    }
    
    private String extractRequestBody(Operation operation, Map<String, Object> parameters) {
        if (operation.getRequestBody() == null) {
            return null;
        }
        
        // 查找body参数
        Object bodyParam = parameters.get("body");
        if (bodyParam != null) {
            return bodyParam instanceof String ? (String) bodyParam : bodyParam.toString();
        }
        
        return null;
    }
    
    private Object processResponse(HttpResponseBean response) {
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCode());
        result.put("headers", response.getHeaders());
        result.put("data", response.getBody());
        
        return result;
    }
    
    /**
     * 内部操作信息类
     */
    private static class OperationInfo {
        final String path;
        final String method;
        final Operation operation;
        
        OperationInfo(String path, String method, Operation operation) {
            this.path = path;
            this.method = method;
            this.operation = operation;
        }
    }
}