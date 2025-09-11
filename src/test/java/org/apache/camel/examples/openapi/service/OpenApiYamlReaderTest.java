package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OpenApiYamlReaderTest {

    @Autowired
    private OpenApiYamlReader openApiYamlReader;

    @Test
    void testReadOpenApiFromResource_Success() {
        // 测试读取示例API文件
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource("classpath:openapi/example-api.yaml");
        
        assertTrue(openAPI.isPresent(), "应该成功读取OpenAPI文件");
        
        OpenAPI api = openAPI.get();
        assertNotNull(api.getInfo(), "API信息不应为空");
        assertEquals("示例HTTP API", api.getInfo().getTitle(), "API标题应该匹配");
        assertEquals("1.0.0", api.getInfo().getVersion(), "API版本应该匹配");
        
        assertNotNull(api.getPaths(), "API路径不应为空");
        assertTrue(api.getPaths().containsKey("/get"), "应该包含/get路径");
        assertTrue(api.getPaths().containsKey("/post"), "应该包含/post路径");
    }

    @Test
    void testReadOpenApiFromResource_FileNotExists() {
        // 测试读取不存在的文件
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource("classpath:openapi/nonexistent.yaml");
        
        assertFalse(openAPI.isPresent(), "不存在的文件应该返回空");
    }

    @Test
    void testReadOpenApiFromResource_EmptyPath() {
        // 测试空路径
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource("");
        
        assertFalse(openAPI.isPresent(), "空路径应该返回空");
    }

    @Test
    void testReadOpenApiFromResource_NullPath() {
        // 测试null路径
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource(null);
        
        assertFalse(openAPI.isPresent(), "null路径应该返回空");
    }
}