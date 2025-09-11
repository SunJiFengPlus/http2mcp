package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI信息配置
 */
@Data
public class InfoConfig {
    /**
     * API标题
     */
    private String title;
    
    /**
     * API描述
     */
    private String description;
    
    /**
     * API版本
     */
    private String version;
}