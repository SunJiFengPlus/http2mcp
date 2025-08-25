package org.apache.camel.examples.service;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP工具服务
 */
@Service
public class McpToolService {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    /**
     * 获取所有可用的工具定义
     */
    public List<McpToolDefinition> getAvailableTools() {
        return Arrays.asList(
            createHttpRequestToolDefinition(),
            createListToolsDefinition(),
            createGetToolInfoDefinition()
        );
    }
    
    /**
     * 执行MCP工具
     */
    public McpToolResponse executeTool(McpToolRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = switch (request.getToolName()) {
                case "http_request" -> executeHttpRequest(request.getArguments());
                case "list_tools" -> executeListTools();
                case "get_tool_info" -> executeGetToolInfo(request.getArguments());
                default -> throw new IllegalArgumentException("未知的工具: " + request.getToolName());
            };
            
            long executionTime = System.currentTimeMillis() - startTime;
            return McpToolResponse.success(request.getRequestId(), request.getToolName(), result, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return McpToolResponse.error(request.getRequestId(), request.getToolName(), e.getMessage(), executionTime);
        }
    }
    
    /**
     * 执行HTTP请求工具
     */
    private HttpResponseBean executeHttpRequest(Map<String, Object> arguments) {
        HttpRequestBean requestBean = buildHttpRequestBean(arguments);
        return producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
    }
    
    /**
     * 执行列出工具
     */
    private List<McpToolDefinition> executeListTools() {
        return getAvailableTools();
    }
    
    /**
     * 执行获取工具信息
     */
    private Optional<McpToolDefinition> executeGetToolInfo(Map<String, Object> arguments) {
        String toolName = (String) arguments.get("tool_name");
        return getAvailableTools().stream()
            .filter(tool -> tool.getName().equals(toolName))
            .findFirst();
    }
    
    /**
     * 构建HTTP请求Bean
     */
    private HttpRequestBean buildHttpRequestBean(Map<String, Object> arguments) {
        HttpRequestBean requestBean = new HttpRequestBean();
        
        Optional.ofNullable((String) arguments.get("url"))
            .ifPresent(requestBean::setUrl);
        
        Optional.ofNullable((String) arguments.get("method"))
            .ifPresent(requestBean::setMethod);
        
        Optional.ofNullable((String) arguments.get("body"))
            .ifPresent(requestBean::setBody);
        
        Optional.ofNullable((Map<String, String>) arguments.get("headers"))
            .ifPresent(requestBean::setHeaders);
        
        Optional.ofNullable((Map<String, String>) arguments.get("query_params"))
            .ifPresent(requestBean::setQueryParams);
        
        return requestBean;
    }
    
    /**
     * 创建HTTP请求工具定义
     */
    private McpToolDefinition createHttpRequestToolDefinition() {
        List<McpToolDefinition.McpToolParameter> parameters = Arrays.asList(
            new McpToolDefinition.McpToolParameter("url", "string", "请求URL", true, null),
            new McpToolDefinition.McpToolParameter("method", "string", "HTTP方法", false, "GET"),
            new McpToolDefinition.McpToolParameter("headers", "object", "请求头", false, new HashMap<>()),
            new McpToolDefinition.McpToolParameter("body", "string", "请求体", false, null),
            new McpToolDefinition.McpToolParameter("query_params", "object", "查询参数", false, new HashMap<>())
        );
        
        return new McpToolDefinition("http_request", "发送HTTP请求", parameters, "network");
    }
    
    /**
     * 创建列出工具定义
     */
    private McpToolDefinition createListToolsDefinition() {
        return new McpToolDefinition("list_tools", "列出所有可用的MCP工具", Collections.emptyList(), "meta");
    }
    
    /**
     * 创建获取工具信息定义
     */
    private McpToolDefinition createGetToolInfoDefinition() {
        List<McpToolDefinition.McpToolParameter> parameters = Collections.singletonList(
            new McpToolDefinition.McpToolParameter("tool_name", "string", "工具名称", true, null)
        );
        
        return new McpToolDefinition("get_tool_info", "获取指定工具的详细信息", parameters, "meta");
    }
}