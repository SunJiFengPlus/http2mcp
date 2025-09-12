package org.apache.camel.examples.web;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.camel.examples.domain.OpenApiTestCase;
import org.apache.camel.examples.domain.TestExecutionResult;
import org.apache.camel.examples.service.OpenApiParserService;
import org.apache.camel.examples.service.OpenApiTestService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI相关功能的REST控制器
 */
@RestController
@RequestMapping("/api/openapi")
public class OpenApiController {
    
    @Autowired
    private OpenApiParserService openApiParserService;
    
    @Autowired
    private OpenApiTestService openApiTestService;
    
    /**
     * 从URL解析OpenAPI文档并生成测试用例
     */
    @PostMapping("/parse-from-url")
    public ResponseEntity<?> parseFromUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("URL不能为空");
            }
            
            OpenAPI openAPI = openApiParserService.parseFromUrl(url.trim());
            if (openAPI == null) {
                return ResponseEntity.badRequest().body("无法解析OpenAPI文档");
            }
            
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            
            return ResponseEntity.ok(Map.of(
                "info", openAPI.getInfo(),
                "testCases", testCases,
                "totalTestCases", testCases.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("解析OpenAPI文档时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 从文件内容解析OpenAPI文档并生成测试用例
     */
    @PostMapping("/parse-from-content")
    public ResponseEntity<?> parseFromContent(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("文档内容不能为空");
            }
            
            OpenAPI openAPI = openApiParserService.parseFromString(content.trim());
            if (openAPI == null) {
                return ResponseEntity.badRequest().body("无法解析OpenAPI文档");
            }
            
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            
            return ResponseEntity.ok(Map.of(
                "info", openAPI.getInfo(),
                "testCases", testCases,
                "totalTestCases", testCases.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("解析OpenAPI文档时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 从上传的文件解析OpenAPI文档并生成测试用例
     */
    @PostMapping("/parse-from-file")
    public ResponseEntity<?> parseFromFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件不能为空");
            }
            
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            OpenAPI openAPI = openApiParserService.parseFromString(content);
            
            if (openAPI == null) {
                return ResponseEntity.badRequest().body("无法解析OpenAPI文档");
            }
            
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            
            return ResponseEntity.ok(Map.of(
                "info", openAPI.getInfo(),
                "testCases", testCases,
                "totalTestCases", testCases.size(),
                "fileName", file.getOriginalFilename()
            ));
            
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("读取文件时发生错误: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("解析OpenAPI文档时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个测试用例
     */
    @PostMapping("/execute-test")
    public ResponseEntity<TestExecutionResult> executeTest(@RequestBody OpenApiTestCase testCase) {
        try {
            TestExecutionResult result = openApiTestService.executeTestCase(testCase);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            TestExecutionResult errorResult = new TestExecutionResult();
            errorResult.setTestCaseName(testCase.getName());
            errorResult.setSuccess(false);
            errorResult.addError("执行测试时发生异常: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }
    
    /**
     * 批量执行测试用例
     */
    @PostMapping("/execute-tests")
    public ResponseEntity<OpenApiTestService.TestSummary> executeTests(@RequestBody List<OpenApiTestCase> testCases) {
        try {
            List<TestExecutionResult> results = openApiTestService.executeTestCases(testCases);
            OpenApiTestService.TestSummary summary = openApiTestService.generateTestSummary(results);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            // 创建一个错误摘要
            OpenApiTestService.TestSummary errorSummary = new OpenApiTestService.TestSummary();
            errorSummary.setTotalTests(testCases.size());
            errorSummary.setFailedTests(testCases.size());
            errorSummary.setSuccessfulTests(0);
            return ResponseEntity.ok(errorSummary);
        }
    }
    
    /**
     * 一站式服务：从URL解析OpenAPI并执行所有测试
     */
    @PostMapping("/parse-and-test")
    public ResponseEntity<?> parseAndTest(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("URL不能为空");
            }
            
            // 解析OpenAPI文档
            OpenAPI openAPI = openApiParserService.parseFromUrl(url.trim());
            if (openAPI == null) {
                return ResponseEntity.badRequest().body("无法解析OpenAPI文档");
            }
            
            // 生成测试用例
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            
            // 执行测试
            List<TestExecutionResult> results = openApiTestService.executeTestCases(testCases);
            OpenApiTestService.TestSummary summary = openApiTestService.generateTestSummary(results);
            
            return ResponseEntity.ok(Map.of(
                "info", openAPI.getInfo(),
                "testSummary", summary
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("处理请求时发生错误: " + e.getMessage());
        }
    }
    
    // Spring AI MCP Tools
    
    /**
     * MCP工具：从URL解析OpenAPI文档并生成测试用例
     */
    @Tool(description = "从URL解析OpenAPI文档并生成测试用例")
    public String parseOpenApiFromUrl(@ToolParam(description = "OpenAPI文档的URL地址") String url) {
        try {
            OpenAPI openAPI = openApiParserService.parseFromUrl(url);
            if (openAPI == null) {
                return "错误：无法解析OpenAPI文档";
            }
            
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            
            StringBuilder result = new StringBuilder();
            result.append("OpenAPI文档解析成功！\n");
            result.append("API标题: ").append(openAPI.getInfo().getTitle()).append("\n");
            result.append("版本: ").append(openAPI.getInfo().getVersion()).append("\n");
            result.append("描述: ").append(openAPI.getInfo().getDescription()).append("\n");
            result.append("生成的测试用例数量: ").append(testCases.size()).append("\n\n");
            
            result.append("测试用例列表:\n");
            for (int i = 0; i < Math.min(testCases.size(), 10); i++) {
                OpenApiTestCase testCase = testCases.get(i);
                result.append(String.format("%d. %s (%s %s)\n", 
                    i + 1, testCase.getName(), testCase.getMethod(), testCase.getPath()));
            }
            
            if (testCases.size() > 10) {
                result.append("... 还有 ").append(testCases.size() - 10).append(" 个测试用例\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "解析OpenAPI文档时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * MCP工具：执行OpenAPI测试
     */
    @Tool(description = "解析OpenAPI文档并执行所有生成的测试用例")
    public String executeOpenApiTests(@ToolParam(description = "OpenAPI文档的URL地址") String url) {
        try {
            // 解析OpenAPI文档
            OpenAPI openAPI = openApiParserService.parseFromUrl(url);
            if (openAPI == null) {
                return "错误：无法解析OpenAPI文档";
            }
            
            // 生成并执行测试用例
            List<OpenApiTestCase> testCases = openApiParserService.generateTestCases(openAPI);
            List<TestExecutionResult> results = openApiTestService.executeTestCases(testCases);
            OpenApiTestService.TestSummary summary = openApiTestService.generateTestSummary(results);
            
            StringBuilder result = new StringBuilder();
            result.append("OpenAPI测试执行完成！\n\n");
            result.append("=== 测试摘要 ===\n");
            result.append("总测试数: ").append(summary.getTotalTests()).append("\n");
            result.append("成功: ").append(summary.getSuccessfulTests()).append("\n");
            result.append("失败: ").append(summary.getFailedTests()).append("\n");
            result.append("成功率: ").append(String.format("%.2f%%", summary.getSuccessRate())).append("\n");
            result.append("总耗时: ").append(summary.getTotalDuration()).append("ms\n\n");
            
            // 显示失败的测试
            if (summary.getFailedTests() > 0) {
                result.append("=== 失败的测试 ===\n");
                for (TestExecutionResult testResult : summary.getResults()) {
                    if (!testResult.isSuccess()) {
                        result.append("- ").append(testResult.getTestCaseName()).append("\n");
                        if (testResult.getErrors() != null) {
                            for (String error : testResult.getErrors()) {
                                result.append("  错误: ").append(error).append("\n");
                            }
                        }
                    }
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "执行OpenAPI测试时发生错误: " + e.getMessage();
        }
    }
}