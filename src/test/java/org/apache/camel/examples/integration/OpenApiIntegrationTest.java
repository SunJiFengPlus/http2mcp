package org.apache.camel.examples.integration;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.camel.examples.service.OpenApiParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * OpenAPI文档解析功能的集成测试
 * 测试OpenApiParserService的各种解析功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OpenApiIntegrationTest {
    
    @Autowired
    private OpenApiParserService openApiParserService;
    
    @Test
    public void testParseOpenApiFromString_ValidYaml() {
        // 测试从YAML字符串解析OpenAPI文档
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
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("A simple API for testing");
        assertThat(openAPI.getPaths()).hasSize(1);
        assertThat(openAPI.getPaths()).containsKey("/get");
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("https://httpbin.org");
    }
    
    @Test
    public void testParseOpenApiFromString_ValidJson() {
        // 测试从JSON字符串解析OpenAPI文档
        String openApiContent = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "JSON Test API",
                "version": "2.0.0"
              },
              "paths": {
                "/test": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "OK"
                      }
                    }
                  }
                }
              }
            }
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(openApiContent);
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("JSON Test API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
        assertThat(openAPI.getPaths()).containsKey("/test");
    }
    
    @Test
    public void testParseOpenApiFromString_EmptyContent() {
        // 测试空内容应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromString("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromString(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromString("   ");
        });
    }
    
    @Test
    public void testParseOpenApiFromString_InvalidContent() {
        // 测试无效内容应该抛出异常
        String invalidContent = "this is not a valid OpenAPI document";
        
        assertThrows(RuntimeException.class, () -> {
            openApiParserService.parseFromString(invalidContent);
        });
    }
    
    @Test
    public void testParseOpenApiFromFile() throws IOException {
        // 创建临时文件测试文件解析
        String openApiContent = """
            openapi: 3.0.0
            info:
              title: File Test API
              version: 1.0.0
            paths:
              /file-test:
                get:
                  responses:
                    '200':
                      description: OK
            """;
        
        Path tempFile = Files.createTempFile("openapi-test", ".yaml");
        Files.write(tempFile, openApiContent.getBytes());
        
        try {
            OpenAPI openAPI = openApiParserService.parseFromFile(tempFile.toString());
            
            assertThat(openAPI).isNotNull();
            assertThat(openAPI.getInfo().getTitle()).isEqualTo("File Test API");
            assertThat(openAPI.getPaths()).containsKey("/file-test");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    public void testParseOpenApiFromFile_InvalidPath() {
        // 测试无效文件路径应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromFile("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromFile(null);
        });
        
        // 测试不存在的文件
        assertThrows(IOException.class, () -> {
            openApiParserService.parseFromFile("/path/that/does/not/exist.yaml");
        });
    }
    
    @Test
    public void testParseOpenApiFromUrl_InvalidUrl() {
        // 测试无效URL应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromUrl("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromUrl(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            openApiParserService.parseFromUrl("   ");
        });
    }
    
    @Test
    public void testIsValidOpenAPI() {
        // 测试有效的OpenAPI文档
        String validContent = """
            openapi: 3.0.0
            info:
              title: Valid API
              version: 1.0.0
            paths: {}
            """;
        
        OpenAPI validOpenAPI = openApiParserService.parseFromString(validContent);
        assertThat(openApiParserService.isValidOpenAPI(validOpenAPI)).isTrue();
        
        // 测试null应该返回false
        assertThat(openApiParserService.isValidOpenAPI(null)).isFalse();
        
        // 测试没有info的OpenAPI应该返回false
        OpenAPI invalidOpenAPI = new OpenAPI();
        assertThat(openApiParserService.isValidOpenAPI(invalidOpenAPI)).isFalse();
    }
    
    @Test
    public void testGetOpenAPIInfo() {
        String openApiContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 2.1.0
              description: This is a test API
            servers:
              - url: https://api.example.com
            paths:
              /users:
                get:
                  responses:
                    '200':
                      description: OK
              /products:
                post:
                  responses:
                    '201':
                      description: Created
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(openApiContent);
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        
        assertThat(info).contains("标题: Test API");
        assertThat(info).contains("版本: 2.1.0");
        assertThat(info).contains("描述: This is a test API");
        assertThat(info).contains("API端点数量: 2");
        assertThat(info).contains("服务器: https://api.example.com");
        
        // 测试null OpenAPI
        String nullInfo = openApiParserService.getOpenAPIInfo(null);
        assertThat(nullInfo).isEqualTo("无效的OpenAPI文档");
    }
    
    @Test
    public void testComplexOpenAPIDocument() {
        // 测试复杂的OpenAPI文档解析
        String complexContent = """
            openapi: 3.0.0
            info:
              title: Complex Pet Store API
              version: 1.0.0
              description: A complex API with multiple paths and operations
            servers:
              - url: https://petstore.swagger.io/v2
              - url: https://staging.petstore.swagger.io/v2
            paths:
              /pets:
                get:
                  summary: List all pets
                  parameters:
                    - name: limit
                      in: query
                      schema:
                        type: integer
                        format: int32
                  responses:
                    '200':
                      description: A list of pets
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              $ref: '#/components/schemas/Pet'
                post:
                  summary: Create a pet
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/Pet'
                  responses:
                    '201':
                      description: Pet created
              /pets/{petId}:
                get:
                  summary: Get pet by ID
                  parameters:
                    - name: petId
                      in: path
                      required: true
                      schema:
                        type: integer
                        format: int64
                  responses:
                    '200':
                      description: Pet details
                    '404':
                      description: Pet not found
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
                    tag:
                      type: string
            """;
        
        OpenAPI openAPI = openApiParserService.parseFromString(complexContent);
        
        // 验证基本信息
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Complex Pet Store API");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getPaths()).hasSize(2);
        
        // 验证路径
        assertThat(openAPI.getPaths()).containsKey("/pets");
        assertThat(openAPI.getPaths()).containsKey("/pets/{petId}");
        
        // 验证操作
        var petsPath = openAPI.getPaths().get("/pets");
        assertThat(petsPath.getGet()).isNotNull();
        assertThat(petsPath.getPost()).isNotNull();
        
        var petByIdPath = openAPI.getPaths().get("/pets/{petId}");
        assertThat(petByIdPath.getGet()).isNotNull();
        
        // 验证组件
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSchemas()).containsKey("Pet");
        
        // 验证有效性
        assertThat(openApiParserService.isValidOpenAPI(openAPI)).isTrue();
        
        // 验证信息摘要
        String info = openApiParserService.getOpenAPIInfo(openAPI);
        assertThat(info).contains("Complex Pet Store API");
        assertThat(info).contains("API端点数量: 2");
    }
}