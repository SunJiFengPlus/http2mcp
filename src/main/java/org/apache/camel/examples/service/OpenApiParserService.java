package org.apache.camel.examples.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.camel.examples.domain.OpenApiTestCase;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI文档解析服务
 */
@Service
public class OpenApiParserService {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * 从URL加载OpenAPI文档
     */
    public OpenAPI parseFromUrl(String url) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        
        return new OpenAPIParser().readLocation(url, null, parseOptions).getOpenAPI();
    }
    
    /**
     * 从文件加载OpenAPI文档
     */
    public OpenAPI parseFromFile(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        return parseFromString(content);
    }
    
    /**
     * 从字符串内容解析OpenAPI文档
     */
    public OpenAPI parseFromString(String content) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        
        return new OpenAPIParser().readContents(content, null, parseOptions).getOpenAPI();
    }
    
    /**
     * 生成基本测试用例
     */
    public List<OpenApiTestCase> generateTestCases(OpenAPI openAPI) {
        List<OpenApiTestCase> testCases = new ArrayList<>();
        
        String baseUrl = getBaseUrl(openAPI);
        
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                testCases.addAll(generateTestCasesForPath(baseUrl, path, pathItem));
            });
        }
        
        return testCases;
    }
    
    private String getBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            return server.getUrl();
        }
        return "http://localhost:8080"; // 默认基础URL
    }
    
    private List<OpenApiTestCase> generateTestCasesForPath(String baseUrl, String path, PathItem pathItem) {
        List<OpenApiTestCase> testCases = new ArrayList<>();
        
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
        
        operations.forEach((httpMethod, operation) -> {
            OpenApiTestCase testCase = new OpenApiTestCase();
            
            // 基本信息
            testCase.setName(generateTestCaseName(httpMethod.name(), path, operation));
            testCase.setDescription(operation.getDescription() != null ? operation.getDescription() : operation.getSummary());
            testCase.setPath(baseUrl + path);
            testCase.setMethod(httpMethod.name());
            testCase.setOperationId(operation.getOperationId());
            
            // 处理参数
            processParameters(testCase, operation);
            
            // 处理请求体
            processRequestBody(testCase, operation);
            
            // 处理期望响应
            processExpectedResponses(testCase, operation);
            
            testCases.add(testCase);
        });
        
        return testCases;
    }
    
    private String generateTestCaseName(String method, String path, Operation operation) {
        if (operation.getOperationId() != null) {
            return operation.getOperationId() + "_test";
        }
        
        String cleanPath = path.replaceAll("[^a-zA-Z0-9]", "_");
        return method.toLowerCase() + cleanPath + "_test";
    }
    
    private void processParameters(OpenApiTestCase testCase, Operation operation) {
        if (operation.getParameters() == null) return;
        
        Map<String, String> headers = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>(); 
        Map<String, String> pathParams = new HashMap<>();
        
        for (Parameter parameter : operation.getParameters()) {
            String exampleValue = generateExampleValue(parameter.getSchema(), parameter.getExample());
            
            switch (parameter.getIn()) {
                case "header":
                    headers.put(parameter.getName(), exampleValue);
                    break;
                case "query":
                    queryParams.put(parameter.getName(), exampleValue);
                    break;
                case "path":
                    pathParams.put(parameter.getName(), exampleValue);
                    // 替换路径中的参数
                    testCase.setPath(testCase.getPath().replace("{" + parameter.getName() + "}", exampleValue));
                    break;
            }
        }
        
        if (!headers.isEmpty()) testCase.setHeaders(headers);
        if (!queryParams.isEmpty()) testCase.setQueryParams(queryParams);
        if (!pathParams.isEmpty()) testCase.setPathParams(pathParams);
    }
    
    private void processRequestBody(OpenApiTestCase testCase, Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) return;
        
        // 优先选择JSON content type
        MediaType mediaType = operation.getRequestBody().getContent().get("application/json");
        String contentType = "application/json";
        
        if (mediaType == null) {
            // 如果没有JSON，选择第一个可用的content type
            Map.Entry<String, MediaType> first = operation.getRequestBody().getContent().entrySet().iterator().next();
            contentType = first.getKey();
            mediaType = first.getValue();
        }
        
        testCase.setContentType(contentType);
        
        if (mediaType.getSchema() != null) {
            String exampleBody = generateExampleBody(mediaType.getSchema(), mediaType.getExample());
            testCase.setRequestBody(exampleBody);
        }
    }
    
    private void processExpectedResponses(OpenApiTestCase testCase, Operation operation) {
        List<OpenApiTestCase.ExpectedResponse> expectedResponses = new ArrayList<>();
        
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((statusCode, apiResponse) -> {
                try {
                    int code = Integer.parseInt(statusCode);
                    OpenApiTestCase.ExpectedResponse expectedResponse = new OpenApiTestCase.ExpectedResponse();
                    expectedResponse.setStatusCode(code);
                    expectedResponse.setDescription(apiResponse.getDescription());
                    
                    // 处理响应内容
                    if (apiResponse.getContent() != null && !apiResponse.getContent().isEmpty()) {
                        Map.Entry<String, MediaType> firstContent = apiResponse.getContent().entrySet().iterator().next();
                        expectedResponse.setContentType(firstContent.getKey());
                    }
                    
                    expectedResponses.add(expectedResponse);
                } catch (NumberFormatException e) {
                    // 忽略非数字状态码（如default）
                }
            });
        }
        
        if (expectedResponses.isEmpty()) {
            // 如果没有定义响应，添加默认的200响应
            OpenApiTestCase.ExpectedResponse defaultResponse = new OpenApiTestCase.ExpectedResponse();
            defaultResponse.setStatusCode(200);
            defaultResponse.setDescription("Success");
            expectedResponses.add(defaultResponse);
        }
        
        testCase.setExpectedResponses(expectedResponses);
    }
    
    private String generateExampleValue(Schema<?> schema, Object example) {
        if (example != null) {
            return example.toString();
        }
        
        if (schema == null) {
            return "example";
        }
        
        if (schema.getExample() != null) {
            return schema.getExample().toString();
        }
        
        // 根据类型生成示例值
        String type = schema.getType();
        if (type == null) {
            return "example";
        }
        
        switch (type.toLowerCase()) {
            case "string":
                return schema.getFormat() != null ? generateStringByFormat(schema.getFormat()) : "example";
            case "integer":
            case "number":
                return "123";
            case "boolean":
                return "true";
            case "array":
                return "[\"example\"]";
            case "object":
                return "{}";
            default:
                return "example";
        }
    }
    
    private String generateStringByFormat(String format) {
        switch (format.toLowerCase()) {
            case "email":
                return "test@example.com";
            case "date":
                return "2023-12-25";
            case "date-time":
                return "2023-12-25T10:30:00Z";
            case "uri":
            case "url":
                return "https://example.com";
            case "uuid":
                return "123e4567-e89b-12d3-a456-426614174000";
            default:
                return "example";
        }
    }
    
    private String generateExampleBody(Schema<?> schema, Object example) {
        if (example != null) {
            try {
                if (example instanceof String) {
                    return (String) example;
                }
                return jsonMapper.writeValueAsString(example);
            } catch (Exception e) {
                // 忽略序列化错误
            }
        }
        
        if (schema == null) {
            return "{}";
        }
        
        if (schema.getExample() != null) {
            try {
                return jsonMapper.writeValueAsString(schema.getExample());
            } catch (Exception e) {
                return "{}";
            }
        }
        
        // 基于schema生成示例JSON
        return generateJsonFromSchema(schema);
    }
    
    private String generateJsonFromSchema(Schema<?> schema) {
        try {
            Object exampleObject = generateObjectFromSchema(schema);
            return jsonMapper.writeValueAsString(exampleObject);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    private Object generateObjectFromSchema(Schema<?> schema) {
        if (schema == null) return null;
        
        String type = schema.getType();
        if (type == null) return null;
        
        switch (type.toLowerCase()) {
            case "object":
                Map<String, Object> obj = new HashMap<>();
                if (schema.getProperties() != null) {
                    schema.getProperties().forEach((propName, propSchema) -> {
                        obj.put(propName, generateObjectFromSchema((Schema<?>) propSchema));
                    });
                }
                return obj;
                
            case "array":
                List<Object> array = new ArrayList<>();
                if (schema.getItems() != null) {
                    array.add(generateObjectFromSchema(schema.getItems()));
                }
                return array;
                
            case "string":
                return generateStringByFormat(schema.getFormat() != null ? schema.getFormat() : "");
                
            case "integer":
            case "number":
                return 123;
                
            case "boolean":
                return true;
                
            default:
                return null;
        }
    }
}