package org.apache.camel.examples.openapi.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI模式配置
 */
@Data
public class SchemaConfig {
    /**
     * 数据类型
     */
    private String type;
    
    /**
     * 格式
     */
    private String format;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 枚举值
     */
    private List<Object> enumValues;
    
    /**
     * 属性（对象类型）
     */
    private Map<String, SchemaConfig> properties;
    
    /**
     * 必需属性列表
     */
    private List<String> required;
    
    /**
     * 数组项模式
     */
    private SchemaConfig items;
    
    /**
     * 引用
     */
    private String ref;
    
    /**
     * 默认值
     */
    private Object defaultValue;
}