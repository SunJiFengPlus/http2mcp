package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI媒体类型配置
 */
@Data
public class MediaTypeConfig {
    /**
     * 模式配置
     */
    private SchemaConfig schema;
    
    /**
     * 示例
     */
    private Object example;
}