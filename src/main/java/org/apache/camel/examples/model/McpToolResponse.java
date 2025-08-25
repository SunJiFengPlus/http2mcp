package org.apache.camel.examples.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * MCP工具响应模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpToolResponse {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 工具名称
     */
    private String toolName;
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 响应数据
     */
    private Object data;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTime;
    
    public static McpToolResponse success(String requestId, String toolName, Object data, long executionTime) {
        return new McpToolResponse(requestId, toolName, true, data, null, executionTime);
    }
    
    public static McpToolResponse error(String requestId, String toolName, String error, long executionTime) {
        return new McpToolResponse(requestId, toolName, false, null, error, executionTime);
    }
}