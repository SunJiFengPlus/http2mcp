package org.apache.camel.examples.openapi.service;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.apache.camel.examples.openapi.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OpenApiMcpService单元测试
 */
@ExtendWith(MockitoExtension.class)
class OpenApiMcpServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private OpenApiMcpService service;

    @BeforeEach
    void setUp() {
        service = new OpenApiMcpService(producerTemplate);
    }

    @Test
    void 应该正确执行GET请求() {
        OpenApiConfig config = createTestOpenApiConfig();
        service.setOpenApiConfig(config);

        HttpResponseBean mockResponse = new HttpResponseBean(200, Map.of(), "{\"id\": 1, \"name\": \"John\"}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Map<String, Object> parameters = Map.of("id", 123);
        
        Object result = service.executeOpenApiTool("getUserById", parameters);

        ArgumentCaptor<HttpRequestBean> requestCaptor = ArgumentCaptor.forClass(HttpRequestBean.class);
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), requestCaptor.capture(), eq(HttpResponseBean.class));

        HttpRequestBean capturedRequest = requestCaptor.getValue();
        
        assertThat(capturedRequest.getMethod()).isEqualTo("GET");
        assertThat(capturedRequest.getUrl()).isEqualTo("https://api.example.com/users/123");

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("statusCode")).isEqualTo(200);
        assertThat(resultMap.get("data")).isEqualTo("{\"id\": 1, \"name\": \"John\"}");
    }

    @Test
    void 应该正确执行包含查询参数的GET请求() {
        OpenApiConfig config = createTestOpenApiConfigWithQueryParams();
        service.setOpenApiConfig(config);

        HttpResponseBean mockResponse = new HttpResponseBean(200, Map.of(), "[]");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Map<String, Object> parameters = Map.of("limit", 10, "offset", 20);
        
        service.executeOpenApiTool("getUsers", parameters);

        ArgumentCaptor<HttpRequestBean> requestCaptor = ArgumentCaptor.forClass(HttpRequestBean.class);
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), requestCaptor.capture(), eq(HttpResponseBean.class));

        HttpRequestBean capturedRequest = requestCaptor.getValue();
        
        assertThat(capturedRequest.getMethod()).isEqualTo("GET");
        assertThat(capturedRequest.getUrl()).isEqualTo("https://api.example.com/users");
        assertThat(capturedRequest.getQueryParams()).containsEntry("limit", "10");
        assertThat(capturedRequest.getQueryParams()).containsEntry("offset", "20");
    }

    @Test
    void 应该正确执行POST请求() {
        OpenApiConfig config = createTestPostOpenApiConfig();
        service.setOpenApiConfig(config);

        HttpResponseBean mockResponse = new HttpResponseBean(201, Map.of(), "{\"id\": 2, \"name\": \"Jane\"}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Map<String, Object> parameters = Map.of(
            "body", "{\"name\": \"Jane\", \"email\": \"jane@example.com\"}"
        );
        
        service.executeOpenApiTool("createUser", parameters);

        ArgumentCaptor<HttpRequestBean> requestCaptor = ArgumentCaptor.forClass(HttpRequestBean.class);
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), requestCaptor.capture(), eq(HttpResponseBean.class));

        HttpRequestBean capturedRequest = requestCaptor.getValue();
        
        assertThat(capturedRequest.getMethod()).isEqualTo("POST");
        assertThat(capturedRequest.getUrl()).isEqualTo("https://api.example.com/users");
        assertThat(capturedRequest.getBody()).isEqualTo("{\"name\": \"Jane\", \"email\": \"jane@example.com\"}");
    }

    @Test
    void 应该获取可用工具列表() {
        OpenApiConfig config = createTestOpenApiConfig();
        service.setOpenApiConfig(config);

        List<OpenApiMcpService.ToolInfo> tools = service.getAvailableTools();

        assertThat(tools).hasSize(1);
        
        OpenApiMcpService.ToolInfo tool = tools.get(0);
        assertThat(tool.getOperationId()).isEqualTo("getUserById");
        assertThat(tool.getMethod()).isEqualTo("GET");
        assertThat(tool.getPath()).isEqualTo("/users/{id}");
        assertThat(tool.getDescription()).isEqualTo("根据ID获取用户");
        assertThat(tool.getParameters()).hasSize(1);
        
        OpenApiMcpService.ParameterInfo param = tool.getParameters().get(0);
        assertThat(param.getName()).isEqualTo("id");
        assertThat(param.getType()).isEqualTo("integer");
        assertThat(param.isRequired()).isTrue();
        assertThat(param.getLocation()).isEqualTo("path");
    }

    @Test
    void 当操作不存在时应该抛出异常() {
        OpenApiConfig config = createTestOpenApiConfig();
        service.setOpenApiConfig(config);

        assertThatThrownBy(() -> service.executeOpenApiTool("nonExistentOperation", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("未找到操作: nonExistentOperation");
    }

    @Test
    void 当配置未设置时应该抛出异常() {
        assertThatThrownBy(() -> service.executeOpenApiTool("getUserById", Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("OpenAPI配置未设置");
    }

    private OpenApiConfig createTestOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.setOpenapi("3.1.0");
        
        InfoConfig info = new InfoConfig();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        config.setInfo(info);
        
        ServerConfig server = new ServerConfig();
        server.setUrl("https://api.example.com");
        config.setServers(List.of(server));
        
        OperationConfig operation = new OperationConfig();
        operation.setOperationId("getUserById");
        operation.setDescription("根据ID获取用户");
        
        ParameterConfig parameter = new ParameterConfig();
        parameter.setName("id");
        parameter.setIn("path");
        parameter.setRequired(true);
        
        SchemaConfig schema = new SchemaConfig();
        schema.setType("integer");
        parameter.setSchema(schema);
        
        operation.setParameters(List.of(parameter));
        
        PathItemConfig pathItem = new PathItemConfig();
        pathItem.setGet(operation);
        
        config.setPaths(Map.of("/users/{id}", pathItem));
        
        return config;
    }

    private OpenApiConfig createTestOpenApiConfigWithQueryParams() {
        OpenApiConfig config = new OpenApiConfig();
        config.setOpenapi("3.1.0");
        
        InfoConfig info = new InfoConfig();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        config.setInfo(info);
        
        ServerConfig server = new ServerConfig();
        server.setUrl("https://api.example.com");
        config.setServers(List.of(server));
        
        OperationConfig operation = new OperationConfig();
        operation.setOperationId("getUsers");
        operation.setDescription("获取用户列表");
        
        ParameterConfig limitParam = new ParameterConfig();
        limitParam.setName("limit");
        limitParam.setIn("query");
        limitParam.setRequired(false);
        SchemaConfig limitSchema = new SchemaConfig();
        limitSchema.setType("integer");
        limitParam.setSchema(limitSchema);
        
        ParameterConfig offsetParam = new ParameterConfig();
        offsetParam.setName("offset");
        offsetParam.setIn("query");
        offsetParam.setRequired(false);
        SchemaConfig offsetSchema = new SchemaConfig();
        offsetSchema.setType("integer");
        offsetParam.setSchema(offsetSchema);
        
        operation.setParameters(List.of(limitParam, offsetParam));
        
        PathItemConfig pathItem = new PathItemConfig();
        pathItem.setGet(operation);
        
        config.setPaths(Map.of("/users", pathItem));
        
        return config;
    }

    private OpenApiConfig createTestPostOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.setOpenapi("3.1.0");
        
        InfoConfig info = new InfoConfig();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        config.setInfo(info);
        
        ServerConfig server = new ServerConfig();
        server.setUrl("https://api.example.com");
        config.setServers(List.of(server));
        
        OperationConfig operation = new OperationConfig();
        operation.setOperationId("createUser");
        operation.setDescription("创建用户");
        
        RequestBodyConfig requestBody = new RequestBodyConfig();
        requestBody.setRequired(true);
        operation.setRequestBody(requestBody);
        
        PathItemConfig pathItem = new PathItemConfig();
        pathItem.setPost(operation);
        
        config.setPaths(Map.of("/users", pathItem));
        
        return config;
    }
}