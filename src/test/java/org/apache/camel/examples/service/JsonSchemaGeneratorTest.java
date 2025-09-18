package org.apache.camel.examples.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JsonSchemaGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenApiParserService openApiParserService = new OpenApiParserService();
    
    private OpenAPI sampleOpenAPI;
    private OpenAPI aiopsOpenAPI;
    private OpenAPI pathVarOpenAPI;

    @BeforeEach
    void setUp() throws IOException {
        // 加载测试资源文件
        String sampleContent = Files.readString(Paths.get("src/test/resources/sample-openapi.yaml"));
        String aiopsContent = Files.readString(Paths.get("src/test/resources/aiops.openapi.json"));
        String pathVarContent = Files.readString(Paths.get("src/test/resources/path-var.json"));
        
        sampleOpenAPI = openApiParserService.parseFromString(sampleContent);
        aiopsOpenAPI = openApiParserService.parseFromString(aiopsContent);
        pathVarOpenAPI = openApiParserService.parseFromString(pathVarContent);
    }

    @Test
    void shouldGenerateSchemaForQueryParametersFromSampleOpenAPI() throws Exception {
        // 从 sample-openapi.yaml 获取 /json 操作的 GET 方法
        Operation operation = sampleOpenAPI.getPaths().get("/json").getGet();
        String path = "/json";
        
        String actualSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        // 预期的 JSON Schema
        String expectedSchema = """
            {
              "type" : "object",
              "properties" : {
                "size" : {
                  "type" : "integer",
                  "description" : "Size of the response"
                },
                "type" : {
                  "type" : "string",
                  "description" : "Response type",
                  "enum" : [ "json", "xml", "html" ]
                }
              },
              "required" : [ ]
            }""";
        
        assertThat(actualSchema).isEqualTo(expectedSchema);
    }

    @Test
    void shouldGenerateSchemaForComplexOperationFromAiopsOpenAPI() throws Exception {
        // 从 aiops.openapi.json 获取复杂的 POST 操作
        Operation operation = aiopsOpenAPI.getPaths().get("/logserviceWeb/api/v1/log-analysis/search").getPost();
        String path = "/logserviceWeb/api/v1/log-analysis/search";
        
        String schemaJson = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        JsonNode schema = objectMapper.readTree(schemaJson);

        assertThat(schema.get("type").asText()).isEqualTo("object");
        assertThat(schema.has("properties")).isTrue();
        assertThat(schema.has("required")).isTrue();

        JsonNode properties = schema.get("properties");
        // 验证查询参数
        assertThat(properties.has("cloudType")).isTrue();
        assertThat(properties.has("dataCenter")).isTrue();
        assertThat(properties.has("envType")).isTrue();
        assertThat(properties.has("vpc")).isTrue();
        assertThat(properties.has("ccsRemote")).isTrue();
        // 验证请求体
        assertThat(properties.has("body")).isTrue();

        JsonNode cloudTypeSchema = properties.get("cloudType");
        assertThat(cloudTypeSchema.get("type").asText()).isEqualTo("string");

        JsonNode bodySchema = properties.get("body");
        assertThat(bodySchema.get("type").asText()).isEqualTo("object");
        assertThat(bodySchema.has("properties")).isTrue();

        JsonNode bodyProperties = bodySchema.get("properties");
        assertThat(bodyProperties.has("startTime")).isTrue();
        assertThat(bodyProperties.has("endTime")).isTrue();
        assertThat(bodyProperties.has("useIndexPattern")).isTrue();
        assertThat(bodyProperties.has("projectId")).isTrue();
        assertThat(bodyProperties.has("serviceName")).isTrue();

        JsonNode required = schema.get("required");
        assertThat(required.size()).isEqualTo(7); // 实际有7个必需参数
        
        // 验证所有必需的参数都存在
        assertThat(required.toString()).contains("cloudType");
        assertThat(required.toString()).contains("dataCenter");
        assertThat(required.toString()).contains("envType");
        assertThat(required.toString()).contains("vpc");
        assertThat(required.toString()).contains("ccsRemote");
        assertThat(required.toString()).contains("Content-Type");
        assertThat(required.toString()).contains("Cookie");
    }

    @Test
    void shouldGenerateSchemaForSimpleGetOperation() {
        // 从 sample-openapi.yaml 获取 /get 操作的 GET 方法
        Operation operation = sampleOpenAPI.getPaths().get("/get").getGet();
        String path = "/get";
        
        String actualSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        // 预期的 JSON Schema
        String expectedSchema = """
            {
              "type" : "object",
              "properties" : {
                "test_param" : {
                  "type" : "string",
                  "description" : "A test parameter"
                }
              },
              "required" : [ ]
            }""";
        
        assertThat(actualSchema).isEqualTo(expectedSchema);
    }

    @Test
    void shouldGenerateSchemaForHeadersOperation() {
        // 从 sample-openapi.yaml 获取 /headers 操作的 GET 方法
        Operation operation = sampleOpenAPI.getPaths().get("/headers").getGet();
        String path = "/headers";
        
        String actualSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        // 预期的 JSON Schema
        String expectedSchema = """
            {
              "type" : "object",
              "properties" : {
                "User-Agent" : {
                  "type" : "string",
                  "description" : "User agent string"
                }
              },
              "required" : [ ]
            }""";
        
        assertThat(actualSchema).isEqualTo(expectedSchema);
    }

    @Test
    void shouldGenerateEmptySchemaForOperationWithoutParameters() {
        // 创建一个没有参数的简单操作
        Operation operation = new Operation();
        operation.setOperationId("getHealth");
        operation.setSummary("Health check endpoint");

        String path = "/health";
        String actualSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        // 预期的 JSON Schema
        String expectedSchema = """
            {
              "type" : "object",
              "properties" : { },
              "required" : [ ]
            }""";
        
        assertThat(actualSchema).isEqualTo(expectedSchema);
    }

    @Test
    void shouldGenerateSchemaForPathVariableOperation() {
        // 从 path-var.json 获取包含路径变量的 POST 操作
        Operation operation = pathVarOpenAPI.getPaths().get("/tracingWeb/tracing-api/api/v1/apm/jvm/services/cpu/instances/{instance}").getPost();
        String path = "/tracingWeb/tracing-api/api/v1/apm/jvm/services/cpu/instances/{instance}";
        
        String actualSchema = JsonSchemaGenerator.generateForOpenApiOperation(operation, path);
        
        // 预期的 JSON Schema
        String expectedSchema = """
            {
              "type" : "object",
              "properties" : {
                "instance" : {
                  "type" : "string",
                  "description" : "实例"
                },
                "end" : {
                  "type" : "string",
                  "description" : "结束时间, 毫秒时间戳"
                },
                "body" : {
                  "type" : "object",
                  "properties" : {
                    "projectId" : {
                      "type" : "string",
                      "description" : "应用, 写死project-dc23000004"
                    },
                    "serviceName" : {
                      "type" : "string",
                      "description" : "服务名"
                    },
                    "serviceVersion" : {
                      "type" : "string",
                      "description" : "服务版本, 写死\\"\\"(空字符串)"
                    }
                  },
                  "required" : [ "projectId", "serviceName", "serviceVersion" ]
                }
              },
              "required" : [ "instance", "end" ]
            }""";
        
        assertThat(actualSchema).isEqualTo(expectedSchema);
    }
}
