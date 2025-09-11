package org.apache.camel.examples.openapi.parser;

import org.apache.camel.examples.openapi.model.OpenApiConfig;
import org.apache.camel.examples.openapi.model.OperationConfig;
import org.apache.camel.examples.openapi.model.PathItemConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * OpenApiParser单元测试
 */
class OpenApiParserTest {

    private final OpenApiParser parser = new OpenApiParser();

    @Test
    void 应该成功解析简单的OpenAPI_YAML配置() {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
              description: 测试API
            servers:
              - url: https://api.example.com
                description: 生产服务器
            paths:
              /users:
                get:
                  operationId: getUsers
                  summary: 获取用户列表
                  description: 获取所有用户的列表
                  responses:
                    '200':
                      description: 成功
            """;

        Optional<OpenApiConfig> result = parser.parseFromContent(yamlContent, true);

        assertThat(result).isPresent();

        OpenApiConfig config = result.get();
        
        assertThat(config.getOpenapi()).isEqualTo("3.1.0");
        assertThat(config.getInfo()).isNotNull();
        assertThat(config.getInfo().getTitle()).isEqualTo("Test API");
        assertThat(config.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(config.getInfo().getDescription()).isEqualTo("测试API");
        
        assertThat(config.getServers()).hasSize(1);
        assertThat(config.getServers().get(0).getUrl()).isEqualTo("https://api.example.com");
        assertThat(config.getServers().get(0).getDescription()).isEqualTo("生产服务器");
        
        assertThat(config.getPaths()).containsKey("/users");
        
        PathItemConfig pathItem = config.getPaths().get("/users");
        assertThat(pathItem.getGet()).isNotNull();
        
        OperationConfig operation = pathItem.getGet();
        assertThat(operation.getOperationId()).isEqualTo("getUsers");
        assertThat(operation.getSummary()).isEqualTo("获取用户列表");
        assertThat(operation.getDescription()).isEqualTo("获取所有用户的列表");
        assertThat(operation.getResponses()).containsKey("200");
    }

    @Test
    void 应该成功解析包含参数的OpenAPI配置() {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
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
                      required: false
                      description: 包含字段
                      schema:
                        type: string
                  responses:
                    '200':
                      description: 成功
            """;

        Optional<OpenApiConfig> result = parser.parseFromContent(yamlContent, true);

        assertThat(result).isPresent();

        OperationConfig operation = result.get().getPaths().get("/users/{id}").getGet();
        
        assertThat(operation.getParameters()).hasSize(2);
        
        assertThat(operation.getParameters().get(0).getName()).isEqualTo("id");
        assertThat(operation.getParameters().get(0).getIn()).isEqualTo("path");
        assertThat(operation.getParameters().get(0).isRequired()).isTrue();
        assertThat(operation.getParameters().get(0).getDescription()).isEqualTo("用户ID");
        assertThat(operation.getParameters().get(0).getSchema().getType()).isEqualTo("integer");
        
        assertThat(operation.getParameters().get(1).getName()).isEqualTo("include");
        assertThat(operation.getParameters().get(1).getIn()).isEqualTo("query");
        assertThat(operation.getParameters().get(1).isRequired()).isFalse();
    }

    @Test
    void 应该成功解析包含请求体的POST操作() {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                post:
                  operationId: createUser
                  summary: 创建用户
                  requestBody:
                    required: true
                    description: 用户信息
                    content:
                      application/json:
                        schema:
                          type: object
                          properties:
                            name:
                              type: string
                            email:
                              type: string
                          required:
                            - name
                            - email
                  responses:
                    '201':
                      description: 用户创建成功
            """;

        Optional<OpenApiConfig> result = parser.parseFromContent(yamlContent, true);

        assertThat(result).isPresent();

        OperationConfig operation = result.get().getPaths().get("/users").getPost();
        
        assertThat(operation.getOperationId()).isEqualTo("createUser");
        assertThat(operation.getRequestBody()).isNotNull();
        assertThat(operation.getRequestBody().isRequired()).isTrue();
        assertThat(operation.getRequestBody().getDescription()).isEqualTo("用户信息");
        
        assertThat(operation.getRequestBody().getContent()).containsKey("application/json");
        
        var mediaType = operation.getRequestBody().getContent().get("application/json");
        assertThat(mediaType.getSchema()).isNotNull();
        assertThat(mediaType.getSchema().getType()).isEqualTo("object");
        assertThat(mediaType.getSchema().getProperties()).containsKey("name");
        assertThat(mediaType.getSchema().getRequired()).containsExactly("name", "email");
    }

    @Test
    void 应该处理无效的OpenAPI内容() {
        String invalidContent = "invalid yaml content";

        Optional<OpenApiConfig> result = parser.parseFromContent(invalidContent, true);

        assertThat(result).isEmpty();
    }

    @Test
    void 应该处理空内容() {
        Optional<OpenApiConfig> result = parser.parseFromContent("", true);

        assertThat(result).isEmpty();
    }
}