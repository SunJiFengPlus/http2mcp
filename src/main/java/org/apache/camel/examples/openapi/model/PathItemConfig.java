package org.apache.camel.examples.openapi.model;

import lombok.Data;

/**
 * OpenAPI路径项配置
 */
@Data
public class PathItemConfig {
    /**
     * GET操作
     */
    private OperationConfig get;
    
    /**
     * POST操作
     */
    private OperationConfig post;
    
    /**
     * PUT操作
     */
    private OperationConfig put;
    
    /**
     * DELETE操作
     */
    private OperationConfig delete;
    
    /**
     * PATCH操作
     */
    private OperationConfig patch;
}