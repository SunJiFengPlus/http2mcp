package org.apache.camel.examples.model;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP工具定义模型
 */
@Data
@AllArgsConstructor
public class McpToolDefinition {
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具描述
     */
    private String description;
    
    /**
     * 工具参数定义
     */
    private List<McpToolParameter> parameters;
    
    /**
     * 工具类别
     */
    private String category;
    
    /**
     * 工具参数定义
     */
    @Data
    @AllArgsConstructor
    public static class McpToolParameter {
        /**
         * 参数名称
         */
        private String name;
        
        /**
         * 参数类型
         */
        private String type;
        
        /**
         * 参数描述
         */
        private String description;
        
        /**
         * 是否必需
         */
        private boolean required;
        
        /**
         * 默认值
         */
        private Object defaultValue;
    }
}