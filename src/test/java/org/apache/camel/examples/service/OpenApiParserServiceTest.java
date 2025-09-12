package org.apache.camel.examples.service;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * OpenApiParserService的单元测试
 * 不依赖Spring容器的单元测试
 */
public class OpenApiParserServiceTest {
    
    private OpenApiParserService openApiParserService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    public void setUp() {
        openApiParserService = new OpenApiParserService();
    }
    
    @Test
    public void testParseFromString_BasicYaml() {
        String yaml = """
            openapi: 3.0.0
            info:
              title: Basic API
              version: 1.0.0
            paths: {}
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(yaml);
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Basic API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }
    
    @Test
    public void testParseFromString_BasicJson() {
        String json = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "JSON API",
                "version": "2.0.0"
              },
              "paths": {}
            }
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(json);
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("JSON API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
    }
    
    @Test
    public void testParseFromString_NullContent() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromString(null)
        );
        
        assertThat(exception.getMessage()).isEqualTo("文档内容不能为空");
    }
    
    @Test
    public void testParseFromString_EmptyContent() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromString("")
        );
        
        assertThat(exception.getMessage()).isEqualTo("文档内容不能为空");
    }
    
    @Test
    public void testParseFromString_WhitespaceContent() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromString("   \n\t  ")
        );
        
        assertThat(exception.getMessage()).isEqualTo("文档内容不能为空");
    }
    
    @Test
    public void testParseFromString_InvalidContent() {
        String invalidContent = "this is definitely not valid OpenAPI content";
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> openApiParserService.parseFromString(invalidContent)
        );
        
        assertThat(exception.getMessage()).isEqualTo("无法解析OpenAPI文档内容");
    }
    
    @Test
    public void testParseFromFile_ValidFile() throws IOException {
        String content = """
            openapi: 3.0.0
            info:
              title: File API
              version: 1.0.0
            paths:
              /test:
                get:
                  responses:
                    '200':
                      description: Success
            """;
        
        Path file = tempDir.resolve("test-api.yaml");
        Files.write(file, content.getBytes());
        
        OpenAPI openAPI = openApiParserService.parseFromFile(file.toString());
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("File API");
        assertThat(openAPI.getPaths()).containsKey("/test");
    }
    
    @Test
    public void testParseFromFile_NullPath() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromFile(null)
        );
        
        assertThat(exception.getMessage()).isEqualTo("文件路径不能为空");
    }
    
    @Test
    public void testParseFromFile_EmptyPath() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromFile("")
        );
        
        assertThat(exception.getMessage()).isEqualTo("文件路径不能为空");
    }
    
    @Test
    public void testParseFromFile_NonExistentFile() {
        assertThrows(
            IOException.class,
            () -> openApiParserService.parseFromFile("/non/existent/file.yaml")
        );
    }
    
    @Test
    public void testParseFromUrl_NullUrl() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromUrl(null)
        );
        
        assertThat(exception.getMessage()).isEqualTo("URL不能为空");
    }
    
    @Test
    public void testParseFromUrl_EmptyUrl() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromUrl("")
        );
        
        assertThat(exception.getMessage()).isEqualTo("URL不能为空");
    }
    
    @Test
    public void testParseFromUrl_WhitespaceUrl() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> openApiParserService.parseFromUrl("   ")
        );
        
        assertThat(exception.getMessage()).isEqualTo("URL不能为空");
    }
    
    @Test
    public void testIsValidOpenAPI_ValidDocument() {
        String content = """
            openapi: 3.0.0
            info:
              title: Valid API
              version: 1.0.0
            paths: {}
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(content);
        
        assertThat(openApiParserService.isValidOpenAPI(openAPI)).isTrue();
    }
    
    @Test
    public void testIsValidOpenAPI_NullDocument() {
        assertThat(openApiParserService.isValidOpenAPI(null)).isFalse();
    }
    
    @Test
    public void testIsValidOpenAPI_NoInfo() {
        OpenAPI openAPI = new OpenAPI();
        
        assertThat(openApiParserService.isValidOpenAPI(openAPI)).isFalse();
    }
    
    @Test
    public void testIsValidOpenAPI_EmptyTitle() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new io.swagger.v3.oas.models.info.Info());
        
        assertThat(openApiParserService.isValidOpenAPI(openAPI)).isFalse();
    }
    
    @Test
    public void testGetOpenAPIInfo_CompleteDocument() {
        String content = """
            openapi: 3.0.0
            info:
              title: Complete API
              version: 2.3.1
              description: A complete API with all details
            servers:
              - url: https://api.complete.com/v1
              - url: https://staging.complete.com/v1
            paths:
              /users:
                get:
                  responses:
                    '200':
                      description: Success
              /products:
                post:
                  responses:
                    '201':
                      description: Created
              /orders:
                get:
                  responses:
                    '200':
                      description: Success
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(content);
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        
        assertThat(info).contains("标题: Complete API");
        assertThat(info).contains("版本: 2.3.1");
        assertThat(info).contains("描述: A complete API with all details");
        assertThat(info).contains("API端点数量: 3");
        assertThat(info).contains("服务器: https://api.complete.com/v1");
    }
    
    @Test
    public void testGetOpenAPIInfo_MinimalDocument() {
        String content = """
            openapi: 3.0.0
            info:
              title: Minimal API
              version: 1.0.0
            paths: {}
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(content);
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        
        assertThat(info).contains("标题: Minimal API");
        assertThat(info).contains("版本: 1.0.0");
        assertThat(info).contains("API端点数量: 0");
        assertThat(info).doesNotContain("描述:");
        assertThat(info).doesNotContain("服务器:");
    }
    
    @Test
    public void testGetOpenAPIInfo_NullDocument() {
        String info = openApiParserService.getOpenAPIInfo(null);
        
        assertThat(info).isEqualTo("无效的OpenAPI文档");
    }
    
    @Test
    public void testGetOpenAPIInfo_NoInfo() {
        OpenAPI openAPI = new OpenAPI();
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        
        assertThat(info).isEqualTo("无效的OpenAPI文档");
    }
    
    @Test
    public void testParseFromString_WithReferences() {
        String content = """
            openapi: 3.0.0
            info:
              title: Reference API
              version: 1.0.0
            paths:
              /pets:
                get:
                  responses:
                    '200':
                      description: A list of pets
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              $ref: '#/components/schemas/Pet'
            components:
              schemas:
                Pet:
                  type: object
                  required:
                    - id
                    - name
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
                    category:
                      $ref: '#/components/schemas/Category'
                Category:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(content);
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Reference API");
        assertThat(openAPI.getComponents().getSchemas()).containsKey("Pet");
        assertThat(openAPI.getComponents().getSchemas()).containsKey("Category");
        
        // 验证引用是否正确解析
        var petSchema = openAPI.getComponents().getSchemas().get("Pet");
        assertThat(petSchema.getRequired()).contains("id", "name");
        assertThat(petSchema.getProperties()).containsKey("id");
        assertThat(petSchema.getProperties()).containsKey("name");
        assertThat(petSchema.getProperties()).containsKey("category");
    }
    
    @Test
    public void testParseFromString_MultipleServers() {
        String content = """
            openapi: 3.0.0
            info:
              title: Multi-Server API
              version: 1.0.0
            servers:
              - url: https://prod.api.com
                description: Production server
              - url: https://staging.api.com
                description: Staging server
              - url: https://dev.api.com
                description: Development server
            paths:
              /health:
                get:
                  responses:
                    '200':
                      description: Health check
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(content);
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getServers()).hasSize(3);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("https://prod.api.com");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("https://staging.api.com");
        assertThat(openAPI.getServers().get(2).getUrl()).isEqualTo("https://dev.api.com");
        
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        // getOpenAPIInfo只显示第一个服务器
        assertThat(info).contains("服务器: https://prod.api.com");
    }
}