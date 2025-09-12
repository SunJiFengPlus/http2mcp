package org.apache.camel.examples.integration;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.camel.examples.domain.OpenApiTestCase;
import org.apache.camel.examples.domain.TestExecutionResult;
import org.apache.camel.examples.service.OpenApiParserService;
import org.apache.camel.examples.service.OpenApiTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI功能的集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OpenApiIntegrationTest {
    
    @Autowired
    private OpenApiParserService openApiParserService;
    
    @Autowired
    private OpenApiTestService openApiTestService;
    
    @Test
    public void testParseOpenApiFromString() {
        // 简单的OpenAPI文档示例
        String openApiContent = """
            openapi: 3.0.0
            info:
              title: Sample API
              description: A simple API for testing
              version: 1.0.0
            servers:
              - url: https://httpbin.org
            paths:
              /get:
                get:
                  summary: Get request test
                  description: Simple GET request for testing
                  responses:
                    '200':
                      description: Successful response
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              url:
                                type: string
                              origin:
                                type: string
            """;
        
        // 解析OpenAPI文档
        OpenAPI openAPI = openApiParserService.parseFromString(openApiContent);
        
        // 验证解析结果
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Sample API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getPaths()).hasSize(1);
        assertThat(openAPI.getPaths()).containsKey("/get");
    }
    
    @Test
    public void testGenerateTestCases() {
        String openApiContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            servers:
              - url: https://httpbin.org
            paths:
              /get:
                get:
                  summary: Get test
                  parameters:
                    - name: test_param
                      in: query
                      required: false
                      schema:
                        type: string
                        example: test_value
                  responses:
                    '200':
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: object
              /post:
                post:
                  summary: Post test
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          type: object
                          properties:
                            name:
                              type: string
                            age:
                              type: integer
                          required:
                            - name
                        example:
                          name: "John"
                          age: 30
                  responses:
                    '200':
                      description: Success
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(openApiContent);
        List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
        
        // 验证生成的测试用例
        assertThat(testCases).hasSize(2);
        
        // 验证GET测试用例
        OpenApiTestCase getTestCase = testCases.stream()
            .filter(tc -> "GET".equals(tc.getMethod()))
            .findFirst()
            .orElse(null);
        
        assertThat(getTestCase).isNotNull();
        assertThat(getTestCase.getPath()).startsWith("https://httpbin.org/get");
        assertThat(getTestCase.getQueryParams()).isNotNull();
        assertThat(getTestCase.getQueryParams()).containsKey("test_param");
        
        // 验证POST测试用例
        OpenApiTestCase postTestCase = testCases.stream()
            .filter(tc -> "POST".equals(tc.getMethod()))
            .findFirst()
            .orElse(null);
        
        assertThat(postTestCase).isNotNull();
        assertThat(postTestCase.getPath()).isEqualTo("https://httpbin.org/post");
        assertThat(postTestCase.getRequestBody()).isNotNull();
        assertThat(postTestCase.getContentType()).isEqualTo("application/json");
    }
    
    @Test
    public void testExecuteSimpleGetTest() {
        // 创建一个简单的GET测试用例
        OpenApiTestCase testCase = new OpenApiTestCase();
        testCase.setName("httpbin_get_test");
        testCase.setDescription("Test httpbin.org GET endpoint");
        testCase.setPath("https://httpbin.org/get");
        testCase.setMethod("GET");
        
        // 添加期望响应
        OpenApiTestCase.ExpectedResponse expectedResponse = new OpenApiTestCase.ExpectedResponse();
        expectedResponse.setStatusCode(200);
        expectedResponse.setDescription("Success");
        expectedResponse.setContentType("application/json");
        testCase.setExpectedResponses(List.of(expectedResponse));
        
        // 执行测试
        TestExecutionResult result = openApiTestService.executeTestCase(testCase);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTestCaseName()).isEqualTo("httpbin_get_test");
        assertThat(result.getActualResponse()).isNotNull();
        assertThat(result.getActualResponse().getStatusCode()).isEqualTo(200);
        assertThat(result.getDurationMs()).isGreaterThan(0);
        
        // httpbin.org 应该正常响应
        assertThat(result.isSuccess()).isTrue();
        if (result.getErrors() != null) {
            System.out.println("Test errors: " + result.getErrors());
        }
        if (result.getWarnings() != null) {
            System.out.println("Test warnings: " + result.getWarnings());
        }
    }
    
    @Test
    public void testExecutePostTestWithBody() {
        // 创建一个POST测试用例
        OpenApiTestCase testCase = new OpenApiTestCase();
        testCase.setName("httpbin_post_test");
        testCase.setDescription("Test httpbin.org POST endpoint");
        testCase.setPath("https://httpbin.org/post");
        testCase.setMethod("POST");
        testCase.setContentType("application/json");
        testCase.setRequestBody("{\"name\":\"test\",\"value\":123}");
        
        // 添加期望响应
        OpenApiTestCase.ExpectedResponse expectedResponse = new OpenApiTestCase.ExpectedResponse();
        expectedResponse.setStatusCode(200);
        expectedResponse.setDescription("Success");
        expectedResponse.setContentType("application/json");
        testCase.setExpectedResponses(List.of(expectedResponse));
        
        // 执行测试
        TestExecutionResult result = openApiTestService.executeTestCase(testCase);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTestCaseName()).isEqualTo("httpbin_post_test");
        assertThat(result.getActualResponse()).isNotNull();
        assertThat(result.getActualResponse().getStatusCode()).isEqualTo(200);
        
        // httpbin.org 应该正常响应
        assertThat(result.isSuccess()).isTrue();
        if (result.getErrors() != null) {
            System.out.println("Test errors: " + result.getErrors());
        }
        if (result.getWarnings() != null) {
            System.out.println("Test warnings: " + result.getWarnings());
        }
    }
    
    @Test
    public void testBatchTestExecution() {
        // 创建多个测试用例
        OpenApiTestCase getTest = new OpenApiTestCase();
        getTest.setName("get_test");
        getTest.setPath("https://httpbin.org/get");
        getTest.setMethod("GET");
        getTest.setExpectedResponses(List.of(
            new OpenApiTestCase.ExpectedResponse(200, "Success", "application/json", null, null)
        ));
        
        OpenApiTestCase statusTest = new OpenApiTestCase();
        statusTest.setName("status_test");
        statusTest.setPath("https://httpbin.org/status/200");
        statusTest.setMethod("GET");
        statusTest.setExpectedResponses(List.of(
            new OpenApiTestCase.ExpectedResponse(200, "Success", null, null, null)
        ));
        
        List<OpenApiTestCase> testCases = List.of(getTest, statusTest);
        
        // 批量执行测试
        List<TestExecutionResult> results = openApiTestService.executeTestCases(testCases);
        
        // 验证结果
        assertThat(results).hasSize(2);
        
        // 生成摘要
        OpenApiTestService.TestSummary summary = openApiTestService.generateTestSummary(results);
        assertThat(summary.getTotalTests()).isEqualTo(2);
        assertThat(summary.getSuccessfulTests()).isGreaterThan(0);
        assertThat(summary.getTotalDuration()).isGreaterThan(0);
        
        System.out.println("批量测试摘要:");
        System.out.println("总测试数: " + summary.getTotalTests());
        System.out.println("成功: " + summary.getSuccessfulTests());
        System.out.println("失败: " + summary.getFailedTests());
        System.out.println("成功率: " + summary.getSuccessRate() + "%");
        System.out.println("总耗时: " + summary.getTotalDuration() + "ms");
    }
    
    @Test
    public void testEndToEndOpenApiWorkflow() {
        // 端到端测试：解析OpenAPI文档并执行所有测试
        String openApiContent = """
            openapi: 3.0.0
            info:
              title: HTTPBin Test API
              version: 1.0.0
            servers:
              - url: https://httpbin.org
            paths:
              /get:
                get:
                  summary: Simple GET request
                  responses:
                    '200':
                      description: Successful response
                      content:
                        application/json:
                          schema:
                            type: object
              /status/200:
                get:
                  summary: Return status 200
                  responses:
                    '200':
                      description: OK
            """;
        
        // 1. 解析OpenAPI文档
        OpenAPI openAPI = openApiParserService.parseFromString(openApiContent);
        assertThat(openAPI).isNotNull();
        
        // 2. 生成测试用例
        List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
        assertThat(testCases).hasSize(2);
        
        // 3. 执行所有测试
        List<TestExecutionResult> results = openApiTestService.executeTestCases(testCases);
        assertThat(results).hasSize(2);
        
        // 4. 验证结果
        OpenApiTestService.TestSummary summary = openApiTestService.generateTestSummary(results);
        
        System.out.println("端到端测试完成:");
        System.out.println("API: " + openAPI.getInfo().getTitle());
        System.out.println("生成的测试用例: " + testCases.size());
        System.out.println("执行结果: " + summary.getSuccessfulTests() + "/" + summary.getTotalTests() + " 成功");
        System.out.println("成功率: " + summary.getSuccessRate() + "%");
        
        // 至少有一个测试应该成功（httpbin.org 通常是可用的）
        assertThat(summary.getSuccessfulTests()).isGreaterThan(0);
    }
}