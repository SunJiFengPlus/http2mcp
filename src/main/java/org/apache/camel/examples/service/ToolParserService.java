package org.apache.camel.examples.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

@Service
public class ToolParserService {
    
    @Resource
    private ProducerTemplate producerTemplate;
    
    public List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> parse(OpenAPI openAPI) {
        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = new ArrayList<>();
        
        if (openAPI == null || openAPI.getPaths() == null) {
            return callbacks;
        }
        
        String baseUrl = getBaseUrl(openAPI);
        return openAPI.getPaths()
            .entrySet()
            .stream()
            .map(pathEntry -> doParse(pathEntry, baseUrl))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    // TODO: 不优雅, 遍历 io.swagger.v3.oas.models.HttpMethod 然后追加
    protected List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> doParse(Map.Entry<String, PathItem> pathEntry, String baseUrl) {
        String path = pathEntry.getKey();
        PathItem pathItem = pathEntry.getValue();

        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = new ArrayList<>();

        // 处理GET操作
        if (pathItem.getGet() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getGet(), "GET", path, baseUrl));
        }

        // 处理POST操作
        if (pathItem.getPost() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getPost(), "POST", path, baseUrl));
        }

        // 处理PUT操作
        if (pathItem.getPut() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getPut(), "PUT", path, baseUrl));
        }

        // 处理DELETE操作
        if (pathItem.getDelete() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getDelete(), "DELETE", path, baseUrl));
        }

        // 处理PATCH操作
        if (pathItem.getPatch() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getPatch(), "PATCH", path, baseUrl));
        }
        
        // HEAD 操作
        if (pathItem.getHead() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getHead(), "HEAD", path, baseUrl));
        }
        
        // TRACE
        if (pathItem.getTrace() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getTrace(), "TRACE", path, baseUrl));
        }
        
        // OPTIONS 操作
        if (pathItem.getOptions() != null) {
            callbacks.add(createFunctionToolCallback(pathItem.getOptions(), "OPTIONS", path, baseUrl));
        }
        
        return callbacks;
    }

    private String getBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            return openAPI.getServers().get(0).getUrl();
        }
        return "http://localhost:8080";
    }
    
    private FunctionToolCallback<Map<String, Object>, HttpResponseBean> createFunctionToolCallback(Operation operation, String method, String path, String baseUrl) {
        String operationId = operation.getOperationId();
        if (operationId == null || operationId.trim().isEmpty()) {
            operationId = method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
        }
        
        String description = operation.getSummary();
        if (description == null || description.trim().isEmpty()) {
            description = operation.getDescription();
        }
        if (description == null || description.trim().isEmpty()) {
            description = method + " " + path;
        }
        
        // 创建HTTP请求处理函数
        Function<Map<String, Object>, HttpResponseBean> httpRequestFunction = createHttpRequestFunction(method, path, baseUrl, operation);
        
        // 生成inputSchema
        String inputSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        return FunctionToolCallback.builder(operationId, httpRequestFunction)
                .description(description)
                .inputType(Map.class)
                .inputSchema(inputSchema)
                .build();
    }
    
    private Function<Map<String, Object>, HttpResponseBean> createHttpRequestFunction(String method, String path, String baseUrl, Operation operation) {
        return (params) -> {
            // 构建完整URL
            String fullUrl = buildFullUrl(baseUrl, path, params);
            
            // 提取参数
            Map<String, String> headers = extractHeaders(params, operation);
            Map<String, String> queryParams = extractQueryParams(params, operation);
            String body = extractRequestBody(params, operation);
            
            // 创建HTTP请求Bean
            HttpRequestBean requestBean = new HttpRequestBean(method, fullUrl, headers, body, queryParams);
            
            // 发送请求
            return producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
        };
    }
    
    private String buildFullUrl(String baseUrl, String path, Map<String, Object> params) {
        String fullPath = path;
        
        // 替换路径参数
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            if (paramValue != null && fullPath.contains("{" + paramName + "}")) {
                fullPath = fullPath.replace("{" + paramName + "}", paramValue.toString());
            }
        }
        
        return baseUrl + fullPath;
    }
    
    private Map<String, String> extractHeaders(Map<String, Object> params, Operation operation) {
        Map<String, String> headers = new HashMap<>();
        
        // 从参数中提取header参数
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if ("header".equals(param.getIn()) && params.containsKey(param.getName())) {
                    Object value = params.get(param.getName());
                    if (value != null) {
                        headers.put(param.getName(), value.toString());
                    }
                }
            }
        }
        
        return headers;
    }
    
    private Map<String, String> extractQueryParams(Map<String, Object> params, Operation operation) {
        Map<String, String> queryParams = new HashMap<>();
        
        // 从参数中提取query参数
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if ("query".equals(param.getIn()) && params.containsKey(param.getName())) {
                    Object value = params.get(param.getName());
                    if (value != null) {
                        queryParams.put(param.getName(), value.toString());
                    }
                }
            }
        }
        
        return queryParams;
    }
    
    private String extractRequestBody(Map<String, Object> params, Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            return null;
        }
        
        // 尝试从参数中获取请求体
        Object bodyParam = params.get("body");
        if (bodyParam != null) {
            if (bodyParam instanceof String) {
                return (String) bodyParam;
            } else {
                // 如果是对象，可以转换为JSON字符串
                return bodyParam.toString();
            }
        }
        
        return null;
    }
}
