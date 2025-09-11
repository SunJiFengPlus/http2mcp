package org.apache.camel.examples.openapi.integration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.examples.openapi.service.OpenApiIndividualToolGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 简化的OpenAPI集成测试
 * 测试每个API接口都成为独立工具的架构
 */
@SpringBootTest
@TestPropertySource(properties = {
    "openapi.config.enabled=false" // 禁用自动配置，避免干扰测试
})
class SimplifiedOpenApiIntegrationTest {

    @Autowired
    private OpenApiIndividualToolGenerator openApiIndividualToolGenerator;

    @Test
    void 应该为每个OpenAPI操作创建独立的工具对象() {
        String testOpenApiYaml = """
            openapi: 3.1.0
            info:
              title: 测试API
              version: 1.0.0
              description: 这是一个用于测试的API
            servers:
              - url: https://httpbin.org
                description: httpbin测试服务器
            paths:
              /get:
                get:
                  operationId: httpbinGet
                  summary: 测试GET请求
                  description: 向httpbin发送GET请求进行测试
                  parameters:
                    - name: param1
                      in: query
                      description: 查询参数1
                      schema:
                        type: string
                  responses:
                    '200':
                      description: 成功响应
              /post:
                post:
                  operationId: httpbinPost
                  summary: 测试POST请求
                  description: 向httpbin发送POST请求进行测试
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          type: object
                          properties:
                            data:
                              type: string
                  responses:
                    '200':
                      description: 成功响应
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(testOpenApiYaml, null, null).getOpenAPI();
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("测试API");
        assertThat(openAPI.getPaths()).hasSize(2);
        
        List<Object> tools = openApiIndividualToolGenerator.createIndividualTools(openAPI);
        
        assertThat(tools).hasSize(2);
        
        var tool1 = (OpenApiIndividualToolGenerator.IndividualApiTool) tools.get(0);
        var tool2 = (OpenApiIndividualToolGenerator.IndividualApiTool) tools.get(1);
        
        assertThat(tool1.getToolName()).isEqualTo("httpbinGet");
        assertThat(tool2.getToolName()).isEqualTo("httpbinPost");
        
        assertThat(tool1.getDescription()).isEqualTo("向httpbin发送GET请求进行测试");
        assertThat(tool2.getDescription()).isEqualTo("向httpbin发送POST请求进行测试");
    }

    @Test
    void 应该为复杂API创建多个独立工具() {
        String complexApiYaml = """
            openapi: 3.1.0
            info:
              title: 复杂API
              version: 1.0.0
            servers:
              - url: https://api.example.com
            paths:
              /users/{userId}/posts/{postId}:
                get:
                  operationId: getUserPost
                  summary: 获取用户的特定帖子
                  parameters:
                    - name: userId
                      in: path
                      required: true
                      description: 用户ID
                      schema:
                        type: integer
                    - name: postId
                      in: path
                      required: true
                      description: 帖子ID
                      schema:
                        type: integer
                  responses:
                    '200':
                      description: 成功
              /users:
                get:
                  operationId: getUsers
                  summary: 获取所有用户
                post:
                  operationId: createUser  
                  summary: 创建用户
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(complexApiYaml, null, null).getOpenAPI();
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("复杂API");
        
        List<Object> tools = openApiIndividualToolGenerator.createIndividualTools(openAPI);
        
        assertThat(tools).hasSize(3);
        
        assertThat(tools)
            .extracting(tool -> ((OpenApiIndividualToolGenerator.IndividualApiTool) tool).getToolName())
            .containsExactlyInAnyOrder("getUserPost", "getUsers", "createUser");
            
        assertThat(tools)
            .extracting(tool -> ((OpenApiIndividualToolGenerator.IndividualApiTool) tool).getDescription())
            .containsExactlyInAnyOrder(
                "获取用户的特定帖子", 
                "获取所有用户", 
                "创建用户"
            );
    }

    @Test
    void 应该正确处理无效的OpenAPI配置() {
        String invalidYaml = """
            invalid: yaml
            content: that should fail
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(invalidYaml, null, null).getOpenAPI();
        
        // OpenAPIV3Parser 会返回 null 对于无效的配置
        assertThat(openAPI).isNull();
    }

    @Test
    void 应该处理空的paths配置() {
        String emptyPathsYaml = """
            openapi: 3.1.0
            info:
              title: 空路径API
              version: 1.0.0
            servers:
              - url: https://api.example.com
            paths: {}
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(emptyPathsYaml, null, null).getOpenAPI();
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("空路径API");
        assertThat(openAPI.getPaths()).isEmpty();

        List<Object> tools = openApiIndividualToolGenerator.createIndividualTools(openAPI);
        
        assertThat(tools).isEmpty();
    }
}