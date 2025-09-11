package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.Map;

/**
 * OpenAPI组件配置
 */
@Data
public class ComponentsConfig {
    /**
     * 模式定义
     */
    private Map<String, SchemaConfig> schemas;
    
    /**
     * 响应定义
     */
    private Map<String, ResponseConfig> responses;
    
    /**
     * 参数定义
     */
    private Map<String, ParameterConfig> parameters;
    
    /**
     * 请求体定义
     */
    private Map<String, RequestBodyConfig> requestBodies;
    
    /**
     * 头定义
     */
    private Map<String, HeaderConfig> headers;
}