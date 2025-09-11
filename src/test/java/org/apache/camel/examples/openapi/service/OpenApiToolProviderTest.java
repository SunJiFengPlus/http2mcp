package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OpenApiToolProvider单元测试
 */
@ExtendWith(MockitoExtension.class)
class OpenApiToolProviderTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private OpenApiToolProvider toolProvider;

    @BeforeEach
    void setUp() {
        String testOpenApiYaml = """
            openapi: 3.1.0
            info:
              title: 测试API
              version: 1.0.0
            servers:
              - url: https://api.example.com
            paths:
              /users/{id}:
                get:
                  operationId: getUserById
                  summary: 根据ID获取用户
                  parameters:
                    - name: id
                      in: path
                      required: true
                      description: 用户ID
                      schema:
                        type: integer
                    - name: include
                      in: query
                      description: 包含字段
                      schema:
                        type: string
                  responses:
                    '200':
                      description: 成功
              /users:
                post:
                  operationId: createUser
                  summary: 创建用户
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          type: object
                          properties:
                            name:
                              type: string
                  responses:
                    '201':
                      description: 创建成功
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(testOpenApiYaml, null, null).getOpenAPI();
        toolProvider = new OpenApiToolProvider(openAPI);
        
        // 手动注入 ProducerTemplate（模拟 Spring 的 @Autowired）
        toolProvider.producerTemplate = producerTemplate;
    }

    @Test
    void 应该正确执行GET请求() {
        HttpResponseBean mockResponse = new HttpResponseBean(200, Map.of(), "{\"id\": 1, \"name\": \"John\"}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Map<String, Object> parameters = Map.of("id", 123, "include", "profile");
        
        Object result = toolProvider.executeOpenApiOperation("getUserById", parameters);

        ArgumentCaptor<HttpRequestBean> requestCaptor = ArgumentCaptor.forClass(HttpRequestBean.class);
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), requestCaptor.capture(), eq(HttpResponseBean.class));

        HttpRequestBean capturedRequest = requestCaptor.getValue();
        
        assertThat(capturedRequest.getMethod()).isEqualTo("GET");
        assertThat(capturedRequest.getUrl()).isEqualTo("https://api.example.com/users/123");
        assertThat(capturedRequest.getQueryParams()).containsEntry("include", "profile");

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("statusCode")).isEqualTo(200);
        assertThat(resultMap.get("data")).isEqualTo("{\"id\": 1, \"name\": \"John\"}");
    }

    @Test
    void 应该正确执行POST请求() {
        HttpResponseBean mockResponse = new HttpResponseBean(201, Map.of(), "{\"id\": 2, \"name\": \"Jane\"}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Map<String, Object> parameters = Map.of(
            "body", "{\"name\": \"Jane\"}"
        );
        
        Object result = toolProvider.executeOpenApiOperation("createUser", parameters);

        ArgumentCaptor<HttpRequestBean> requestCaptor = ArgumentCaptor.forClass(HttpRequestBean.class);
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), requestCaptor.capture(), eq(HttpResponseBean.class));

        HttpRequestBean capturedRequest = requestCaptor.getValue();
        
        assertThat(capturedRequest.getMethod()).isEqualTo("POST");
        assertThat(capturedRequest.getUrl()).isEqualTo("https://api.example.com/users");
        assertThat(capturedRequest.getBody()).isEqualTo("{\"name\": \"Jane\"}");

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("statusCode")).isEqualTo(201);
    }

    @Test
    void 当操作不存在时应该返回错误() {
        Object result = toolProvider.executeOpenApiOperation("nonExistentOperation", Map.of());

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("error")).isEqualTo(true);
        assertThat(resultMap.get("message")).asString().contains("未找到操作");
    }

    @Test
    void 应该正确处理空参数() {
        HttpResponseBean mockResponse = new HttpResponseBean(200, Map.of(), "{}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
            .thenReturn(mockResponse);

        Object result = toolProvider.executeOpenApiOperation("getUserById", null);

        // 即使参数为null，也应该能处理（内部会转为空Map）
        verify(producerTemplate).requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class));
        
        assertThat(result).isInstanceOf(Map.class);
    }
}