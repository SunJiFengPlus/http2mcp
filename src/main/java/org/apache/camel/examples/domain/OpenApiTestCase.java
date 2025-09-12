package org.apache.camel.examples.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 基于OpenAPI生成的测试用例
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiTestCase {
    private String name;
    private String description;
    private String path;
    private String method;
    private String operationId;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> pathParams;
    private String requestBody;
    private String contentType;
    private List<ExpectedResponse> expectedResponses;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExpectedResponse {
        private int statusCode;
        private String description;
        private String contentType;
        private String expectedBodyPattern; // regex pattern or exact match
        private Map<String, String> expectedHeaders;
    }
}