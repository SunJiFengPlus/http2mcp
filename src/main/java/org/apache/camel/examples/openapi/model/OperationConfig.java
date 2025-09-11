package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI操作配置
 */
@Data
public class OperationConfig {
    /**
     * 操作ID，将作为工具名称
     */
    private String operationId;
    
    /**
     * 操作摘要
     */
    private String summary;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 参数配置
     */
    private List<ParameterConfig> parameters;
    
    /**
     * 请求体配置
     */
    private RequestBodyConfig requestBody;
    
    /**
     * 响应配置
     */
    private Map<String, ResponseConfig> responses;
    
    /**
     * 标签
     */
    private List<String> tags;
}