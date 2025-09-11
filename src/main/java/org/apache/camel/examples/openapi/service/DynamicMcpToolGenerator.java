package org.apache.camel.examples.openapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.examples.openapi.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态MCP工具生成器
 * 基于OpenAPI配置动态生成带有@Tool注解的方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpToolGenerator {
    
    private final OpenApiMcpService openApiMcpService;
    
    /**
     * 基于OpenAPI配置生成动态工具对象
     */
    public Object generateDynamicTools(OpenApiConfig openApiConfig) {
        log.info("开始生成基于OpenAPI配置的动态MCP工具");
        
        // 设置OpenAPI配置到服务中
        openApiMcpService.setOpenApiConfig(openApiConfig);
        
        // 获取所有可用的工具
        List<OpenApiMcpService.ToolInfo> toolInfos = openApiMcpService.getAvailableTools();
        
        if (toolInfos.isEmpty()) {
            log.warn("未找到任何可用的OpenAPI操作");
            return new EmptyToolsProxy();
        }
        
        log.info("找到 {} 个OpenAPI操作，开始生成动态工具", toolInfos.size());
        
        // 创建动态代理对象
        return Proxy.newProxyInstance(
            DynamicMcpToolGenerator.class.getClassLoader(),
            new Class<?>[]{DynamicTools.class},
            (proxy, method, args) -> {
                String methodName = method.getName();
                log.debug("调用动态工具方法: {}", methodName);
                
                // 查找对应的工具信息
                Optional<OpenApiMcpService.ToolInfo> toolInfo = toolInfos.stream()
                    .filter(info -> methodName.equals(sanitizeOperationId(info.getOperationId())))
                    .findFirst();
                
                if (toolInfo.isEmpty()) {
                    throw new IllegalArgumentException("未找到操作: " + methodName);
                }
                
                // 构建参数映射
                Map<String, Object> parameters = new HashMap<>();
                if (args != null && args.length > 0 && args[0] instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramMap = (Map<String, Object>) args[0];
                    parameters.putAll(paramMap);
                }
                
                // 执行工具调用
                return openApiMcpService.executeOpenApiTool(toolInfo.get().getOperationId(), parameters);
            }
        );
    }
    
    /**
     * 清理操作ID，使其适合作为方法名
     */
    private String sanitizeOperationId(String operationId) {
        if (operationId == null || operationId.trim().isEmpty()) {
            return "unknownOperation";
        }
        
        // 移除非法字符，保留字母、数字和下划线
        return operationId.replaceAll("[^a-zA-Z0-9_]", "")
                         .replaceAll("^[0-9]", "_$0"); // 如果以数字开头，添加下划线前缀
    }
    
    /**
     * 获取动态生成的工具列表（用于调试和文档）
     */
    public List<ToolDescription> getGeneratedToolDescriptions(OpenApiConfig openApiConfig) {
        openApiMcpService.setOpenApiConfig(openApiConfig);
        
        return openApiMcpService.getAvailableTools().stream()
            .map(toolInfo -> new ToolDescription(
                sanitizeOperationId(toolInfo.getOperationId()),
                toolInfo.getDescription(),
                toolInfo.getMethod() + " " + toolInfo.getPath(),
                toolInfo.getParameters().stream()
                    .map(param -> new ParameterDescription(
                        param.getName(),
                        param.getDescription(),
                        param.getType(),
                        param.isRequired()
                    ))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * 动态工具接口（标记接口）
     */
    public interface DynamicTools {
        // 动态生成的方法将通过代理实现
    }
    
    /**
     * 空工具代理（当没有OpenAPI操作时使用）
     */
    public static class EmptyToolsProxy implements DynamicTools {
        @Tool(description = "没有可用的OpenAPI工具")
        public String noToolsAvailable() {
            return "当前没有可用的OpenAPI工具。请检查OpenAPI配置文件。";
        }
    }
    
    /**
     * 工具描述
     */
    public static class ToolDescription {
        private final String methodName;
        private final String description;
        private final String endpoint;
        private final List<ParameterDescription> parameters;
        
        public ToolDescription(String methodName, String description, String endpoint, List<ParameterDescription> parameters) {
            this.methodName = methodName;
            this.description = description;
            this.endpoint = endpoint;
            this.parameters = parameters;
        }
        
        // Getters
        public String getMethodName() { return methodName; }
        public String getDescription() { return description; }
        public String getEndpoint() { return endpoint; }
        public List<ParameterDescription> getParameters() { return parameters; }
        
        @Override
        public String toString() {
            return String.format("Tool{method='%s', description='%s', endpoint='%s', params=%d}",
                methodName, description, endpoint, parameters.size());
        }
    }
    
    /**
     * 参数描述
     */
    public static class ParameterDescription {
        private final String name;
        private final String description;
        private final String type;
        private final boolean required;
        
        public ParameterDescription(String name, String description, String type, boolean required) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.required = required;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        
        @Override
        public String toString() {
            return String.format("Param{name='%s', type='%s', required=%s}",
                name, type, required);
        }
    }
}