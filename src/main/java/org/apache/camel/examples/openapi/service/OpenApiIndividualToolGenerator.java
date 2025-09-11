package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * OpenAPI个体工具生成器
 * 为每个OpenAPI操作创建独立的工具对象
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiIndividualToolGenerator {
    
    private final ProducerTemplate producerTemplate;
    
    /**
     * 从OpenAPI规范创建工具对象列表
     */
    public List<Object> createIndividualTools(OpenAPI openAPI) {
        if (openAPI == null || openAPI.getPaths() == null) {
            log.warn("OpenAPI规范为空，返回空的工具列表");
            return Collections.emptyList();
        }
        
        String baseUrl = extractBaseUrl(openAPI);
        List<Object> tools = new ArrayList<>();
        
        // 为每个路径的每个操作创建独立的工具对象
        openAPI.getPaths().forEach((path, pathItem) -> {
            tools.addAll(createToolsForPath(path, pathItem, baseUrl));
        });
        
        log.info("为OpenAPI创建了 {} 个独立工具", tools.size());
        
        return tools;
    }
    
    /**
     * 为单个路径项的所有操作创建工具对象
     */
    private List<Object> createToolsForPath(String path, PathItem pathItem, String baseUrl) {
        List<Object> tools = new ArrayList<>();
        
        if (pathItem.getGet() != null) {
            tools.add(new IndividualApiTool(path, "GET", pathItem.getGet(), baseUrl, producerTemplate));
        }
        if (pathItem.getPost() != null) {
            tools.add(new IndividualApiTool(path, "POST", pathItem.getPost(), baseUrl, producerTemplate));
        }
        if (pathItem.getPut() != null) {
            tools.add(new IndividualApiTool(path, "PUT", pathItem.getPut(), baseUrl, producerTemplate));
        }
        if (pathItem.getDelete() != null) {
            tools.add(new IndividualApiTool(path, "DELETE", pathItem.getDelete(), baseUrl, producerTemplate));
        }
        if (pathItem.getPatch() != null) {
            tools.add(new IndividualApiTool(path, "PATCH", pathItem.getPatch(), baseUrl, producerTemplate));
        }
        
        return tools;
    }
    
    /**
     * 提取基础URL
     */
    private String extractBaseUrl(OpenAPI openAPI) {
        return Optional.ofNullable(openAPI.getServers())
            .filter(servers -> !servers.isEmpty())
            .map(servers -> servers.get(0).getUrl())
            .orElse("");
    }
    
    /**
     * 个体API工具类
     * 每个实例代表一个具体的API操作
     */
    public static class IndividualApiTool {
        
        private final String path;
        private final String method;
        private final Operation operation;
        private final String baseUrl;
        private final ProducerTemplate producerTemplate;
        private final String toolName;
        
        public IndividualApiTool(String path, String method, Operation operation, 
                               String baseUrl, ProducerTemplate producerTemplate) {
            this.path = path;
            this.method = method;
            this.operation = operation;
            this.baseUrl = baseUrl;
            this.producerTemplate = producerTemplate;
            this.toolName = operation.getOperationId() != null ? 
                operation.getOperationId() : 
                generateToolName(method, path);
        }
        
        /**
         * 动态工具方法 - Spring AI会通过反射找到这个@Tool方法
         */
        @Tool(description = "动态生成的OpenAPI工具")
        public Object executeOperation(
                @ToolParam(description = "操作参数") Map<String, Object> parameters) {
            
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            
            try {
                log.info("执行OpenAPI工具: {} ({} {})", toolName, method, path);
                
                // 构建HTTP请求
                String fullUrl = buildFullUrl(path, parameters);
                Map<String, String> headers = extractHeaders(operation, parameters);
                Map<String, String> queryParams = extractQueryParams(operation, parameters);
                String body = extractRequestBody(operation, parameters);
                
                HttpRequestBean request = new HttpRequestBean(method, fullUrl, headers, body, queryParams);
                
                log.debug("发送HTTP请求: {} {}", method, fullUrl);
                
                HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
                
                return processResponse(response);
                
            } catch (Exception e) {
                log.error("执行OpenAPI工具失败: {}", toolName, e);
                return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "tool", toolName
                );
            }
        }
        
        private String buildFullUrl(String path, Map<String, Object> arguments) {
            String processedPath = path;
            
            // 替换路径参数
            if (operation.getParameters() != null) {
                for (Parameter param : operation.getParameters()) {
                    if ("path".equals(param.getIn()) && arguments.containsKey(param.getName())) {
                        String placeholder = "{" + param.getName() + "}";
                        String value = String.valueOf(arguments.get(param.getName()));
                        processedPath = processedPath.replace(placeholder, value);
                    }
                }
            }
            
            return baseUrl + processedPath;
        }
        
        private Map<String, String> extractHeaders(Operation operation, Map<String, Object> arguments) {
            Map<String, String> headers = new HashMap<>();
            
            if (operation.getParameters() != null) {
                operation.getParameters().stream()
                    .filter(param -> "header".equals(param.getIn()))
                    .forEach(param -> {
                        Object value = arguments.get(param.getName());
                        if (value != null) {
                            headers.put(param.getName(), String.valueOf(value));
                        }
                    });
            }
            
            return headers;
        }
        
        private Map<String, String> extractQueryParams(Operation operation, Map<String, Object> arguments) {
            Map<String, String> queryParams = new HashMap<>();
            
            if (operation.getParameters() != null) {
                operation.getParameters().stream()
                    .filter(param -> "query".equals(param.getIn()))
                    .forEach(param -> {
                        Object value = arguments.get(param.getName());
                        if (value != null) {
                            queryParams.put(param.getName(), String.valueOf(value));
                        }
                    });
            }
            
            return queryParams;
        }
        
        private String extractRequestBody(Operation operation, Map<String, Object> arguments) {
            if (operation.getRequestBody() == null) {
                return null;
            }
            
            // 查找body参数
            Object bodyParam = arguments.get("body");
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
         * 生成工具名称（当operationId不存在时）
         */
        private String generateToolName(String method, String path) {
            // 将路径转换为小写命名
            String cleanPath = path.replaceAll("\\{[^}]*\\}", "")  // 移除路径参数
                                  .replaceAll("[^a-zA-Z0-9]", "_")   // 替换特殊字符
                                  .replaceAll("_+", "_")             // 合并多个下划线
                                  .replaceAll("^_|_$", "")           // 移除首尾下划线
                                  .toLowerCase();                    // 转为小写
            
            return method.toLowerCase() + 
                   (cleanPath.isEmpty() ? "" : "_" + cleanPath);
        }
        
        // Getter方法供测试使用
        public String getToolName() { return toolName; }
        public String getPath() { return path; }
        public String getMethod() { return method; }
        public String getDescription() { 
            return Optional.ofNullable(operation.getDescription())
                .orElse(Optional.ofNullable(operation.getSummary())
                    .orElse(method + " " + path));
        }
    }
}