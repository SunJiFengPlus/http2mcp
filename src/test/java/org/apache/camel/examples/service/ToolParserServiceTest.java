package org.apache.camel.examples.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpRequestBean;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.io.ClassPathResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolParserServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private ToolParserService toolParserService;
    private OpenApiParserService openApiParserService;

    @BeforeEach
    void setUp() {
        toolParserService = new ToolParserService();
        openApiParserService = new OpenApiParserService();
        
        // 使用反射设置私有字段
        try {
            java.lang.reflect.Field field = ToolParserService.class.getDeclaredField("producerTemplate");
            field.setAccessible(true);
            field.set(toolParserService, producerTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock ProducerTemplate", e);
        }
    }

    @Test
    void testParseWithSimpleGetOperation() throws Exception {
        // 从YAML文件读取OpenAPI对象
        OpenAPI openAPI = loadOpenAPIFromYaml();
        
        // 模拟HTTP响应
        HttpResponseBean mockResponse = new HttpResponseBean(200, new HashMap<>(), "{\"message\": \"success\"}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
                .thenReturn(mockResponse);

        // 执行测试
        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = toolParserService.parse(openAPI);

        // 验证结果 - 应该包含3个操作：getJson, getHeaders, simpleGet
        assertNotNull(callbacks);
        assertEquals(3, callbacks.size());

        // 测试getJson操作 - 由于FunctionToolCallback没有getName方法，我们直接测试第一个回调
        FunctionToolCallback<Map<String, Object>, HttpResponseBean> getJsonCallback = callbacks.get(0);
        assertNotNull(getJsonCallback);

        // 执行回调函数 - 根据用户示例，call方法接受String参数
        assertDoesNotThrow(() -> getJsonCallback.call("{\"size\": 10, \"type\": \"json\"}"));
    }

    @Test
    void testParseWithHeadersOperation() throws Exception {
        // 从YAML文件读取OpenAPI对象
        OpenAPI openAPI = loadOpenAPIFromYaml();
        
        // 模拟HTTP响应
        HttpResponseBean mockResponse = new HttpResponseBean(200, new HashMap<>(), "{\"headers\": {\"User-Agent\": \"HTTPBin-Test/1.0\"}}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
                .thenReturn(mockResponse);

        // 执行测试
        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = toolParserService.parse(openAPI);

        // 验证结果
        assertNotNull(callbacks);
        assertEquals(3, callbacks.size());

        // 测试getHeaders操作 - 由于FunctionToolCallback没有getName方法，我们直接测试第二个回调
        FunctionToolCallback<Map<String, Object>, HttpResponseBean> getHeadersCallback = callbacks.get(1);
        assertNotNull(getHeadersCallback);

        // 执行回调函数 - 根据用户示例，call方法接受String参数
        assertDoesNotThrow(() -> getHeadersCallback.call("{\"User-Agent\": \"HTTPBin-Test/1.0\"}"));
    }

    @Test
    void testParseWithQueryParameters() throws Exception {
        // 从YAML文件读取OpenAPI对象
        OpenAPI openAPI = loadOpenAPIFromYaml();
        
        // 模拟HTTP响应
        HttpResponseBean mockResponse = new HttpResponseBean(200, new HashMap<>(), "{\"args\": {\"test_param\": \"test_value\"}}");
        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(HttpRequestBean.class), eq(HttpResponseBean.class)))
                .thenReturn(mockResponse);

        // 执行测试
        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = toolParserService.parse(openAPI);

        // 验证结果
        assertNotNull(callbacks);
        assertEquals(3, callbacks.size());

        // 测试simpleGet操作 - 由于FunctionToolCallback没有getName方法，我们直接测试第三个回调
        FunctionToolCallback<Map<String, Object>, HttpResponseBean> simpleGetCallback = callbacks.get(2);
        assertNotNull(simpleGetCallback);

        // 执行回调函数 - 根据用户示例，call方法接受String参数
        assertDoesNotThrow(() -> simpleGetCallback.call("{\"test_param\": \"test_value\"}"));
    }

    @Test
    void testParseWithNullOpenAPI() {
        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = toolParserService.parse(null);
        assertNotNull(callbacks);
        assertTrue(callbacks.isEmpty());
    }

    @Test
    void testParseWithEmptyPaths() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("Test API").version("1.0.0"));
        // 不设置paths

        List<FunctionToolCallback<Map<String, Object>, HttpResponseBean>> callbacks = toolParserService.parse(openAPI);
        assertNotNull(callbacks);
        assertTrue(callbacks.isEmpty());
    }

    /**
     * 从YAML文件加载OpenAPI对象
     */
    private OpenAPI loadOpenAPIFromYaml() throws Exception {
        ClassPathResource resource = new ClassPathResource("sample-openapi.yaml");
        String yamlContent = new String(resource.getInputStream().readAllBytes());
        return openApiParserService.parseFromString(yamlContent);
    }
}
