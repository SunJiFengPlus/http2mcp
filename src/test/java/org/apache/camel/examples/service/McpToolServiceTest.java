package org.apache.camel.examples.service;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpToolServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @InjectMocks
    private McpToolService mcpToolService;

    private HttpResponseBean mockHttpResponse;

    @BeforeEach
    void setUp() {
        mockHttpResponse = new HttpResponseBean(200, new HashMap<>(), "{\"success\": true}");
    }

    @Test
    void shouldReturnAvailableTools() {
        List<McpToolDefinition> tools = mcpToolService.getAvailableTools();

        assertThat(tools).isNotNull();
        assertThat(tools).hasSize(3);
        
        List<String> toolNames = tools.stream()
            .map(McpToolDefinition::getName)
            .toList();
        
        assertThat(toolNames).containsExactlyInAnyOrder("http_request", "list_tools", "get_tool_info");
    }

    @Test
    void shouldExecuteHttpRequestTool() {
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockHttpResponse);

        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-1");
        request.setToolName("http_request");
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("url", "http://example.com");
        arguments.put("method", "GET");
        request.setArguments(arguments);

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("test-1");
        assertThat(response.getToolName()).isEqualTo("http_request");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(mockHttpResponse);
        assertThat(response.getExecutionTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldExecuteListToolsWithEmptyArguments() {
        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-2");
        request.setToolName("list_tools");
        request.setArguments(new HashMap<>());

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("test-2");
        assertThat(response.getToolName()).isEqualTo("list_tools");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(List.class);
        
        @SuppressWarnings("unchecked")
        List<McpToolDefinition> tools = (List<McpToolDefinition>) response.getData();
        assertThat(tools).hasSize(3);
    }

    @Test
    void shouldExecuteGetToolInfoWithValidToolName() {
        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-3");
        request.setToolName("get_tool_info");
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("tool_name", "http_request");
        request.setArguments(arguments);

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("test-3");
        assertThat(response.getToolName()).isEqualTo("get_tool_info");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(java.util.Optional.class);
        
        @SuppressWarnings("unchecked")
        java.util.Optional<McpToolDefinition> toolOpt = (java.util.Optional<McpToolDefinition>) response.getData();
        assertThat(toolOpt).isPresent();
        assertThat(toolOpt.get().getName()).isEqualTo("http_request");
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownToolName() {
        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-4");
        request.setToolName("get_tool_info");
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("tool_name", "unknown_tool");
        request.setArguments(arguments);

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("test-4");
        assertThat(response.getToolName()).isEqualTo("get_tool_info");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(java.util.Optional.class);
        
        @SuppressWarnings("unchecked")
        java.util.Optional<McpToolDefinition> toolOpt = (java.util.Optional<McpToolDefinition>) response.getData();
        assertThat(toolOpt).isEmpty();
    }

    @Test
    void shouldReturnErrorForUnknownTool() {
        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-5");
        request.setToolName("unknown_tool");
        request.setArguments(new HashMap<>());

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo("test-5");
        assertThat(response.getToolName()).isEqualTo("unknown_tool");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("未知的工具");
        assertThat(response.getData()).isNull();
        assertThat(response.getExecutionTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldReturnHttpRequestToolDefinition() {
        List<McpToolDefinition> tools = mcpToolService.getAvailableTools();
        
        McpToolDefinition httpTool = tools.stream()
            .filter(tool -> "http_request".equals(tool.getName()))
            .findFirst()
            .orElse(null);

        assertThat(httpTool).isNotNull();
        assertThat(httpTool.getName()).isEqualTo("http_request");
        assertThat(httpTool.getDescription()).isEqualTo("发送HTTP请求");
        assertThat(httpTool.getCategory()).isEqualTo("network");
        assertThat(httpTool.getParameters()).hasSize(5);
        
        List<String> paramNames = httpTool.getParameters().stream()
            .map(McpToolDefinition.McpToolParameter::getName)
            .toList();
        
        assertThat(paramNames).containsExactlyInAnyOrder("url", "method", "headers", "body", "query_params");
    }

    @Test
    void shouldBuildHttpRequestBeanFromArguments() {
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockHttpResponse);

        McpToolRequest request = new McpToolRequest();
        request.setRequestId("test-6");
        request.setToolName("http_request");
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("url", "http://test.com");
        arguments.put("method", "POST");
        arguments.put("body", "{\"test\": true}");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        arguments.put("headers", headers);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("param1", "value1");
        arguments.put("query_params", queryParams);
        
        request.setArguments(arguments);

        McpToolResponse response = mcpToolService.executeTool(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(mockHttpResponse);
    }
}