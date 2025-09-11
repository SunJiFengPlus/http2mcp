package org.apache.camel.examples.openapi.integration;

import org.apache.camel.examples.openapi.config.OpenApiMcpConfig;
import org.apache.camel.examples.openapi.parser.OpenApiParser;
import org.apache.camel.examples.openapi.service.DynamicMcpToolGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * OpenAPI MCP集成测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "openapi.config.enabled=false" // 禁用自动配置，避免干扰测试
})
class OpenApiMcpIntegrationTest {

    @Autowired
    private OpenApiParser openApiParser;
    
    @Autowired
    private DynamicMcpToolGenerator dynamicMcpToolGenerator;
    
    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void 应该成功创建完整的OpenAPI_MCP流程() throws IOException {
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
                      content:
                        application/json:
                          schema:
                            type: object
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

        var configOptional = openApiParser.parseFromContent(testOpenApiYaml, true);
        
        assertThat(configOptional).isPresent();

        var config = configOptional.get();
        assertThat(config.getInfo().getTitle()).isEqualTo("测试API");
        assertThat(config.getPaths()).hasSize(2);
        
        var dynamicTools = dynamicMcpToolGenerator.generateDynamicTools(config);
        
        assertThat(dynamicTools).isNotNull();

        var toolDescriptions = dynamicMcpToolGenerator.getGeneratedToolDescriptions(config);
        
        assertThat(toolDescriptions).hasSize(2);
        
        assertThat(toolDescriptions)
            .extracting(DynamicMcpToolGenerator.ToolDescription::getMethodName)
            .containsExactlyInAnyOrder("httpbinGet", "httpbinPost");
            
        assertThat(toolDescriptions)
            .extracting(DynamicMcpToolGenerator.ToolDescription::getEndpoint)
            .containsExactlyInAnyOrder("GET /get", "POST /post");
    }

    @Test
    void 应该处理复杂的参数配置() {
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
                    - name: include
                      in: query
                      description: 包含的额外信息
                      schema:
                        type: array
                        items:
                          type: string
                    - name: format
                      in: query
                      description: 响应格式
                      schema:
                        type: string
                        enum: [json, xml, yaml]
                        default: json
                    - name: Authorization
                      in: header
                      description: 认证令牌
                      schema:
                        type: string
                  responses:
                    '200':
                      description: 成功
            """;

        var configOptional = openApiParser.parseFromContent(complexApiYaml, true);
        
        assertThat(configOptional).isPresent();

        var config = configOptional.get();
        var toolDescriptions = dynamicMcpToolGenerator.getGeneratedToolDescriptions(config);
        
        assertThat(toolDescriptions).hasSize(1);
        
        var toolDescription = toolDescriptions.get(0);
        assertThat(toolDescription.getMethodName()).isEqualTo("getUserPost");
        assertThat(toolDescription.getEndpoint()).isEqualTo("GET /users/{userId}/posts/{postId}");
        assertThat(toolDescription.getParameters()).hasSize(5);
        
        assertThat(toolDescription.getParameters())
            .extracting(DynamicMcpToolGenerator.ParameterDescription::getName)
            .containsExactlyInAnyOrder("userId", "postId", "include", "format", "Authorization");
            
        assertThat(toolDescription.getParameters())
            .filteredOn(p -> "userId".equals(p.getName()))
            .extracting(DynamicMcpToolGenerator.ParameterDescription::isRequired)
            .containsExactly(true);
            
        assertThat(toolDescription.getParameters())
            .filteredOn(p -> "include".equals(p.getName()))
            .extracting(DynamicMcpToolGenerator.ParameterDescription::isRequired)
            .containsExactly(false);
    }

    @Test
    void 应该正确处理无效的OpenAPI配置() {
        String invalidYaml = """
            invalid: yaml
            content: that should fail
            """;

        var configOptional = openApiParser.parseFromContent(invalidYaml, true);
        
        assertThat(configOptional).isEmpty();
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

        var configOptional = openApiParser.parseFromContent(emptyPathsYaml, true);
        
        assertThat(configOptional).isPresent();

        var config = configOptional.get();
        var toolDescriptions = dynamicMcpToolGenerator.getGeneratedToolDescriptions(config);
        
        assertThat(toolDescriptions).isEmpty();
    }
}