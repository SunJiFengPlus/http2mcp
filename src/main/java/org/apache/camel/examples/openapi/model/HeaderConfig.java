package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI头配置
 */
@Data
public class HeaderConfig {
    /**
     * 头描述
     */
    private String description;
    
    /**
     * 头模式
     */
    private SchemaConfig schema;
    
    /**
     * 是否必需
     */
    private boolean required;
}