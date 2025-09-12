package org.apache.camel.examples.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试执行结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestExecutionResult {
    private String testCaseName;
    private boolean success;
    private LocalDateTime executionTime;
    private long durationMs;
    private HttpRequestBean actualRequest;
    private HttpResponseBean actualResponse;
    private List<String> errors;
    private List<String> warnings;
    private OpenApiTestCase.ExpectedResponse expectedResponse;
    
    public void addError(String error) {
        if (errors == null) {
            errors = new java.util.ArrayList<>();
        }
        errors.add(error);
        this.success = false;
    }
    
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new java.util.ArrayList<>();
        }
        warnings.add(warning);
    }
}