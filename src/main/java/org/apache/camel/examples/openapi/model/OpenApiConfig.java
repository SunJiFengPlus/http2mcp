package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI配置模型类
 */
@Data
public class OpenApiConfig {
    /**
     * OpenAPI版本
     */
    private String openapi;
    
    /**
     * 基本信息
     */
    private InfoConfig info;
    
    /**
     * 服务器配置
     */
    private List<ServerConfig> servers;
    
    /**
     * 路径配置
     */
    private Map<String, PathItemConfig> paths;
    
    /**
     * 组件配置（schemas等）
     */
    private ComponentsConfig components;
}