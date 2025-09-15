package org.apache.camel.examples.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OpenAPI到MCP工具的转换服务
 * 将OpenAPI规范动态转换为MCP工具方法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiToMcpToolsService {

    private final ProducerTemplate producerTemplate;
    private final OpenApiParserService openApiParserService;
    
    // 存储已注册的工具信息
    private final Map<String, ToolInfo> registeredTools = new ConcurrentHashMap<>();
    
    /**
     * 工具信息存储类
     */
    public static class ToolInfo {
        public final String operationId;
        public final String method;
        public final String path;
        public final String baseUrl;
        public final String description;
        public final List<ParameterInfo> parameters;
        public final boolean hasRequestBody;
        
        public ToolInfo(String operationId, String method, String path, String baseUrl, 
                       String description, List<ParameterInfo> parameters, boolean hasRequestBody) {
            this.operationId = operationId;
            this.method = method;
            this.path = path;
            this.baseUrl = baseUrl;
            this.description = description;
            this.parameters = parameters;
            this.hasRequestBody = hasRequestBody;
        }
    }
    
    /**
     * 参数信息存储类
     */
    public static class ParameterInfo {
        public final String name;
        public final String type;
        public final String location; // query, path, header
        public final boolean required;
        public final String description;
        
        public ParameterInfo(String name, String type, String location, boolean required, String description) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.required = required;
            this.description = description;
        }
    }

    /**
     * 从OpenAPI文件注册工具
     */
    public void registerToolsFromOpenApiFile(String filePath) {
        try {
            OpenAPI openAPI = openApiParserService.parseFromFile(filePath);
            registerToolsFromOpenApi(openAPI);
        } catch (Exception e) {
            log.error("从OpenAPI文件注册工具失败: {}", filePath, e);
            throw new RuntimeException("注册OpenAPI工具失败", e);
        }
    }

    /**
     * 从OpenAPI对象注册工具
     */
    public void registerToolsFromOpenApi(OpenAPI openAPI) {
        if (!openApiParserService.isValidOpenAPI(openAPI)) {
            throw new IllegalArgumentException("无效的OpenAPI规范");
        }

        String baseUrl = getBaseUrl(openAPI);
        
        if (openAPI.getPaths() == null) {
            log.warn("OpenAPI规范中没有定义路径");
            return;
        }

        openAPI.getPaths().forEach((path, pathItem) -> {
            registerOperationsForPath(path, pathItem, baseUrl);
        });

        log.info("成功注册 {} 个OpenAPI工具", registeredTools.size());
    }

    /**
     * 注册路径下的所有操作
     */
    private void registerOperationsForPath(String path, PathItem pathItem, String baseUrl) {
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
        
        operations.forEach((httpMethod, operation) -> {
            if (operation.getOperationId() != null) {
                registerOperation(httpMethod.name(), path, operation, baseUrl);
            } else {
                log.warn("跳过没有operationId的操作: {} {}", httpMethod, path);
            }
        });
    }

    /**
     * 注册单个操作
     */
    private void registerOperation(String method, String path, Operation operation, String baseUrl) {
        String operationId = operation.getOperationId();
        String description = operation.getSummary() != null ? 
            operation.getSummary() : 
            (operation.getDescription() != null ? operation.getDescription() : "从OpenAPI生成的工具");

        List<ParameterInfo> parameters = extractParameters(operation);
        boolean hasRequestBody = operation.getRequestBody() != null;

        ToolInfo toolInfo = new ToolInfo(operationId, method, path, baseUrl, description, parameters, hasRequestBody);
        registeredTools.put(operationId, toolInfo);
        
        log.debug("注册工具: {} {} {}", method, path, operationId);
    }

    /**
     * 提取操作参数
     */
    private List<ParameterInfo> extractParameters(Operation operation) {
        List<ParameterInfo> parameters = new ArrayList<>();
        
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                String type = getParameterType(param);
                parameters.add(new ParameterInfo(
                    param.getName(),
                    type,
                    param.getIn(),
                    param.getRequired() != null && param.getRequired(),
                    param.getDescription() != null ? param.getDescription() : ""
                ));
            }
        }
        
        return parameters;
    }

    /**
     * 获取参数类型
     */
    private String getParameterType(Parameter param) {
        if (param.getSchema() == null) {
            return "string";
        }
        String type = param.getSchema().getType();
        return type != null ? type : "string";
    }

    /**
     * 获取基础URL
     */
    private String getBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            return server.getUrl();
        }
        return "http://localhost:8080"; // 默认值
    }

    /**
     * 获取已注册的工具信息
     */
    public Map<String, ToolInfo> getRegisteredTools() {
        return new HashMap<>(registeredTools);
    }

    /**
     * 清空已注册的工具
     */
    public void clearRegisteredTools() {
        registeredTools.clear();
        log.info("已清空所有注册的工具");
    }

    // === 动态生成的MCP工具方法示例 ===
    // 以下方法展示了如何为特定的OpenAPI操作创建MCP工具

    /**
     * 示例：列出宠物的MCP工具
     * 这个方法演示了如何为OpenAPI中的 GET /pets 操作创建MCP工具
     */
    @Tool(description = "列出所有宠物")
    public HttpResponseBean listPets(
            @ToolParam(description = "返回多少条记录（最大100）", required = false) Integer limit,
            @ToolParam(description = "宠物状态过滤", required = false) String status) {
        
        ToolInfo toolInfo = registeredTools.get("listPets");
        if (toolInfo == null) {
            log.error("工具 listPets 未注册");
            return createErrorResponse("工具未注册");
        }

        return executeHttpRequest(toolInfo, createQueryParams(limit, status), null, null);
    }

    /**
     * 示例：创建宠物的MCP工具
     * 这个方法演示了如何为OpenAPI中的 POST /pets 操作创建MCP工具
     */
    @Tool(description = "创建新宠物")
    public HttpResponseBean createPet(
            @ToolParam(description = "宠物名称") String name,
            @ToolParam(description = "宠物标签", required = false) String tag) {
        
        ToolInfo toolInfo = registeredTools.get("createPet");
        if (toolInfo == null) {
            log.error("工具 createPet 未注册");
            return createErrorResponse("工具未注册");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        if (tag != null) {
            requestBody.put("tag", tag);
        }

        return executeHttpRequest(toolInfo, null, null, requestBody);
    }

    /**
     * 通用HTTP请求执行器
     */
    private HttpResponseBean executeHttpRequest(ToolInfo toolInfo, 
                                              Map<String, String> queryParams,
                                              Map<String, String> headers,
                                              Object requestBody) {
        try {
            String url = buildUrl(toolInfo.baseUrl, toolInfo.path);
            
            HttpRequestBean requestBean = new HttpRequestBean(
                toolInfo.method,
                url,
                headers,
                requestBody != null ? convertToJsonString(requestBody) : null,
                queryParams
            );

            return producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
        } catch (Exception e) {
            log.error("执行HTTP请求失败: {} {}", toolInfo.method, toolInfo.path, e);
            return createErrorResponse("请求执行失败: " + e.getMessage());
        }
    }

    /**
     * 构建完整URL
     */
    private String buildUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    /**
     * 创建查询参数
     */
    private Map<String, String> createQueryParams(Object... params) {
        Map<String, String> queryParams = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length && params[i] != null && params[i + 1] != null) {
                queryParams.put(params[i].toString(), params[i + 1].toString());
            }
        }
        return queryParams.isEmpty() ? null : queryParams;
    }

    /**
     * 转换对象为JSON字符串
     */
    private String convertToJsonString(Object obj) {
        // 这里应该使用JSON库进行序列化，为了简单起见，暂时使用toString
        if (obj instanceof Map) {
            try {
                // 简单的JSON序列化，实际项目中应使用Jackson
                Map<?, ?> map = (Map<?, ?>) obj;
                return map.entrySet().stream()
                    .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(",", "{", "}"));
            } catch (Exception e) {
                log.warn("JSON序列化失败，使用toString", e);
                return obj.toString();
            }
        }
        return obj.toString();
    }

    /**
     * 创建错误响应
     */
    private HttpResponseBean createErrorResponse(String message) {
        HttpResponseBean response = new HttpResponseBean();
        response.setStatusCode(500);
        response.setBody("{\"error\":\"" + message + "\"}");
        return response;
    }
}