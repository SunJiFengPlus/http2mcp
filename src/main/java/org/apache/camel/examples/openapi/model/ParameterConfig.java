package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI参数配置
 */
@Data
public class ParameterConfig {
    /**
     * 参数名称
     */
    private String name;
    
    /**
     * 参数位置（query, header, path, cookie）
     */
    private String in;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 是否必需
     */
    private boolean required;
    
    /**
     * 参数模式
     */
    private SchemaConfig schema;
}