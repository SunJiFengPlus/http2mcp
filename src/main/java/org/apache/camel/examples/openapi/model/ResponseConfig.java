package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.Map;

/**
 * OpenAPI响应配置
 */
@Data
public class ResponseConfig {
    /**
     * 响应描述
     */
    private String description;
    
    /**
     * 响应内容配置
     */
    private Map<String, MediaTypeConfig> content;
    
    /**
     * 响应头配置
     */
    private Map<String, HeaderConfig> headers;
}