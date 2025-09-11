package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI服务器配置
 */
@Data
public class ServerConfig {
    /**
     * 服务器URL
     */
    private String url;
    
    /**
     * 服务器描述
     */
    private String description;
}