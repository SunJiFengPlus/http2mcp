package org.apache.camel.examples.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI文档规范的域模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiSpec {
    private String version;
    private OpenApiInfo info;
    private String basePath;
    private List<String> servers;
    private Map<String, OpenApiPath> paths;
    private Map<String, OpenApiSchema> schemas;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiInfo {
        private String title;
        private String description;
        private String version;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiPath {
        private Map<String, OpenApiOperation> operations; // get, post, put, delete等
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiOperation {
        private String operationId;
        private String summary;
        private String description;
        private List<OpenApiParameter> parameters;
        private OpenApiRequestBody requestBody;
        private Map<String, OpenApiResponse> responses;
        private List<String> tags;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiParameter {
        private String name;
        private String in; // path, query, header, cookie
        private String description;
        private boolean required;
        private OpenApiSchema schema;
        private Object example;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiRequestBody {
        private String description;
        private boolean required;
        private Map<String, OpenApiMediaType> content;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiResponse {
        private String description;
        private Map<String, OpenApiMediaType> content;
        private Map<String, OpenApiHeader> headers;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiMediaType {
        private OpenApiSchema schema;
        private Object example;
        private Map<String, Object> examples;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiHeader {
        private String description;
        private OpenApiSchema schema;
        private Object example;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenApiSchema {
        private String type;
        private String format;
        private String description;
        private Object example;
        private List<String> required;
        private Map<String, OpenApiSchema> properties;
        private OpenApiSchema items; // for array type
        private String ref; // $ref reference
        private List<Object> enumValues; // enum values
        private Object defaultValue;
        private Number minimum;
        private Number maximum;
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
    }
}