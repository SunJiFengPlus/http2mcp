package org.apache.camel.examples.service;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.apache.camel.examples.domain.OpenApiTestCase;
import org.apache.camel.examples.domain.TestExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * OpenAPI测试执行服务
 */
@Service
public class OpenApiTestService {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    /**
     * 执行单个测试用例
     */
    public TestExecutionResult executeTestCase(OpenApiTestCase testCase) {
        TestExecutionResult result = new TestExecutionResult();
        result.setTestCaseName(testCase.getName());
        result.setExecutionTime(LocalDateTime.now());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建HTTP请求
            HttpRequestBean request = buildHttpRequest(testCase);
            result.setActualRequest(request);
            
            // 执行HTTP请求
            HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
            result.setActualResponse(response);
            
            // 验证响应
            validateResponse(result, testCase, response);
            
        } catch (Exception e) {
            result.addError("执行测试时发生异常: " + e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            result.setDurationMs(endTime - startTime);
        }
        
        return result;
    }
    
    /**
     * 批量执行测试用例
     */
    public List<TestExecutionResult> executeTestCases(List<OpenApiTestCase> testCases) {
        List<TestExecutionResult> results = new ArrayList<>();
        
        for (OpenApiTestCase testCase : testCases) {
            TestExecutionResult result = executeTestCase(testCase);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 构建HTTP请求
     */
    private HttpRequestBean buildHttpRequest(OpenApiTestCase testCase) {
        Map<String, String> headers = new HashMap<>();
        if (testCase.getHeaders() != null) {
            headers.putAll(testCase.getHeaders());
        }
        
        // 设置Content-Type
        if (testCase.getContentType() != null && !testCase.getContentType().isEmpty()) {
            headers.put("Content-Type", testCase.getContentType());
        }
        
        Map<String, String> queryParams = new HashMap<>();
        if (testCase.getQueryParams() != null) {
            queryParams.putAll(testCase.getQueryParams());
        }
        
        return new HttpRequestBean(
            testCase.getMethod(),
            testCase.getPath(),
            headers,
            testCase.getRequestBody(),
            queryParams
        );
    }
    
    /**
     * 验证响应
     */
    private void validateResponse(TestExecutionResult result, OpenApiTestCase testCase, HttpResponseBean response) {
        boolean foundMatchingExpectedResponse = false;
        
        if (testCase.getExpectedResponses() != null) {
            for (OpenApiTestCase.ExpectedResponse expectedResponse : testCase.getExpectedResponses()) {
                if (expectedResponse.getStatusCode() == response.getStatusCode()) {
                    foundMatchingExpectedResponse = true;
                    result.setExpectedResponse(expectedResponse);
                    
                    // 验证状态码
                    if (response.getStatusCode() != expectedResponse.getStatusCode()) {
                        result.addError(String.format("状态码不匹配. 期望: %d, 实际: %d", 
                            expectedResponse.getStatusCode(), response.getStatusCode()));
                    }
                    
                    // 验证Content-Type
                    if (expectedResponse.getContentType() != null) {
                        String actualContentType = getContentTypeFromHeaders(response.getHeaders());
                        if (!matchesContentType(actualContentType, expectedResponse.getContentType())) {
                            result.addWarning(String.format("Content-Type可能不匹配. 期望: %s, 实际: %s", 
                                expectedResponse.getContentType(), actualContentType));
                        }
                    }
                    
                    // 验证响应体模式
                    if (expectedResponse.getExpectedBodyPattern() != null) {
                        validateBodyPattern(result, response.getBody(), expectedResponse.getExpectedBodyPattern());
                    }
                    
                    // 验证响应头
                    if (expectedResponse.getExpectedHeaders() != null) {
                        validateHeaders(result, response.getHeaders(), expectedResponse.getExpectedHeaders());
                    }
                    
                    break;
                }
            }
        }
        
        if (!foundMatchingExpectedResponse) {
            result.addWarning(String.format("没有找到匹配状态码 %d 的期望响应定义", response.getStatusCode()));
        }
        
        // 如果没有错误，标记为成功
        if (result.getErrors() == null || result.getErrors().isEmpty()) {
            result.setSuccess(true);
        }
    }
    
    private String getContentTypeFromHeaders(Map<String, Object> headers) {
        if (headers == null) return null;
        
        // 尝试多种可能的Content-Type header名称
        String[] possibleKeys = {"Content-Type", "content-type", "CONTENT-TYPE"};
        
        for (String key : possibleKeys) {
            Object value = headers.get(key);
            if (value != null) {
                String contentType = value.toString();
                // 只取分号前的部分
                int semicolon = contentType.indexOf(';');
                return semicolon > 0 ? contentType.substring(0, semicolon).trim() : contentType.trim();
            }
        }
        
        return null;
    }
    
    private boolean matchesContentType(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        
        // 简单的Content-Type匹配，忽略charset等参数
        String actualBase = actual.split(";")[0].trim().toLowerCase();
        String expectedBase = expected.split(";")[0].trim().toLowerCase();
        
        return actualBase.equals(expectedBase);
    }
    
    private void validateBodyPattern(TestExecutionResult result, String actualBody, String expectedPattern) {
        if (actualBody == null) {
            if (expectedPattern != null && !expectedPattern.isEmpty()) {
                result.addError("响应体为空，但期望有内容");
            }
            return;
        }
        
        if (expectedPattern == null || expectedPattern.isEmpty()) {
            return;
        }
        
        try {
            // 如果模式以 "regex:" 开头，使用正则匹配
            if (expectedPattern.startsWith("regex:")) {
                String regex = expectedPattern.substring(6);
                if (!Pattern.matches(regex, actualBody)) {
                    result.addError(String.format("响应体不匹配正则表达式: %s", regex));
                }
            } else {
                // 否则使用精确匹配
                if (!actualBody.equals(expectedPattern)) {
                    result.addError("响应体不匹配期望内容");
                }
            }
        } catch (Exception e) {
            result.addWarning("验证响应体模式时发生错误: " + e.getMessage());
        }
    }
    
    private void validateHeaders(TestExecutionResult result, Map<String, Object> actualHeaders, 
                               Map<String, String> expectedHeaders) {
        if (expectedHeaders == null || expectedHeaders.isEmpty()) return;
        if (actualHeaders == null) {
            result.addError("响应中没有headers，但期望有特定的headers");
            return;
        }
        
        for (Map.Entry<String, String> expected : expectedHeaders.entrySet()) {
            String expectedKey = expected.getKey();
            String expectedValue = expected.getValue();
            
            Object actualValue = findHeaderValue(actualHeaders, expectedKey);
            
            if (actualValue == null) {
                result.addError(String.format("缺少期望的响应header: %s", expectedKey));
            } else if (!expectedValue.equals(actualValue.toString())) {
                result.addWarning(String.format("响应header值不匹配. Header: %s, 期望: %s, 实际: %s", 
                    expectedKey, expectedValue, actualValue));
            }
        }
    }
    
    private Object findHeaderValue(Map<String, Object> headers, String headerName) {
        // 大小写不敏感的header查找
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * 生成测试报告摘要
     */
    public TestSummary generateTestSummary(List<TestExecutionResult> results) {
        TestSummary summary = new TestSummary();
        summary.setTotalTests(results.size());
        summary.setSuccessfulTests((int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum());
        summary.setFailedTests(summary.getTotalTests() - summary.getSuccessfulTests());
        summary.setTotalDuration(results.stream().mapToLong(TestExecutionResult::getDurationMs).sum());
        summary.setResults(results);
        
        return summary;
    }
    
    /**
     * 测试摘要类
     */
    public static class TestSummary {
        private int totalTests;
        private int successfulTests;
        private int failedTests;
        private long totalDuration;
        private List<TestExecutionResult> results;
        
        // getters and setters
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        
        public int getSuccessfulTests() { return successfulTests; }
        public void setSuccessfulTests(int successfulTests) { this.successfulTests = successfulTests; }
        
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        
        public long getTotalDuration() { return totalDuration; }
        public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
        
        public List<TestExecutionResult> getResults() { return results; }
        public void setResults(List<TestExecutionResult> results) { this.results = results; }
        
        public double getSuccessRate() {
            return totalTests > 0 ? (double) successfulTests / totalTests * 100 : 0;
        }
    }
}