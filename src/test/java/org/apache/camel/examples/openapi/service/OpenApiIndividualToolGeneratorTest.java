package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * OpenApiIndividualToolGenerator单元测试
 */
@ExtendWith(MockitoExtension.class)
class OpenApiIndividualToolGeneratorTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private OpenApiIndividualToolGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OpenApiIndividualToolGenerator(producerTemplate);
    }

    @Test
    void 应该为每个OpenAPI操作创建独立的工具对象() {
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
              /users:
                get:
                  operationId: getUsers
                  summary: 获取所有用户
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
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(testOpenApiYaml, null, null).getOpenAPI();
        
        List<Object> tools = generator.createIndividualTools(openAPI);
        
        assertThat(tools).hasSize(3);

        assertThat(tools)
            .extracting(tool -> ((OpenApiIndividualToolGenerator.IndividualApiTool) tool).getToolName())
            .containsExactlyInAnyOrder("getUserById", "getUsers", "createUser");

        assertThat(tools)
            .extracting(tool -> ((OpenApiIndividualToolGenerator.IndividualApiTool) tool).getDescription())
            .containsExactlyInAnyOrder(
                "根据ID获取用户", 
                "获取所有用户", 
                "创建用户"
            );
    }

    @Test
    void 应该正确处理空的OpenAPI规范() {
        List<Object> tools = generator.createIndividualTools(null);
        
        assertThat(tools).isEmpty();
    }

    @Test
    void 应该为没有operationId的操作生成工具名称() {
        String noOperationIdYaml = """
            openapi: 3.1.0
            info:
              title: 无操作ID的API
              version: 1.0.0
            servers:
              - url: https://api.example.com
            paths:
              /test:
                get:
                  summary: 测试接口
                  description: 没有operationId的测试接口
            """;

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(noOperationIdYaml, null, null).getOpenAPI();
        
        List<Object> tools = generator.createIndividualTools(openAPI);
        
        assertThat(tools).hasSize(1);
        
        var tool = (OpenApiIndividualToolGenerator.IndividualApiTool) tools.get(0);
        assertThat(tool.getToolName()).isEqualTo("get_test");
        assertThat(tool.getDescription()).isEqualTo("没有operationId的测试接口");
    }
}