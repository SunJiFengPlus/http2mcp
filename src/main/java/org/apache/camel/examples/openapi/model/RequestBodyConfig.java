package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.Map;

/**
 * OpenAPI请求体配置
 */
@Data
public class RequestBodyConfig {
    /**
     * 请求体描述
     */
    private String description;
    
    /**
     * 是否必需
     */
    private boolean required;
    
    /**
     * 内容配置
     */
    private Map<String, MediaTypeConfig> content;
}