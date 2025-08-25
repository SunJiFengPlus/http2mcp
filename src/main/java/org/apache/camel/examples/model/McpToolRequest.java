package org.apache.camel.examples.model;

import lombok.Data;

import java.util.Map;

/**
 * MCP工具请求模型
 */
@Data
public class McpToolRequest {
    
    /**
     * 工具名称
     */
    private String toolName;
    
    /**
     * 工具参数
     */
    private Map<String, Object> arguments;
    
    /**
     * 请求ID，用于跟踪请求
     */
    private String requestId;
}