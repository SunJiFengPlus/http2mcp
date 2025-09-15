package org.apache.camel.examples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.domain.HttpResponseBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 动态MCP工具生成器的测试
 */
@ExtendWith(MockitoExtension.class)
public class DynamicMcpToolsGeneratorTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private DynamicMcpToolsGenerator dynamicMcpToolsGenerator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dynamicMcpToolsGenerator = new DynamicMcpToolsGenerator(producerTemplate, objectMapper);
    }

    @Test
    void testLoadOpenApiSpec_Success() {
        // Arrange
        OpenAPI mockOpenAPI = createDetailedMockOpenAPI();

        // Act
        dynamicMcpToolsGenerator.loadOpenApiSpec(mockOpenAPI);

        // Assert
        assertThat(dynamicMcpToolsGenerator.getToolDefinitionsCount()).isGreaterThan(0);
        assertThat(dynamicMcpToolsGenerator.getCurrentOpenAPI()).isEqualTo(mockOpenAPI);
    }

    @Test
    void testLoadOpenApiSpec_EmptyPaths() {
        // Arrange
        OpenAPI openAPIWithEmptyPaths = new OpenAPI();
        io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();
        info.setTitle("Empty API");
        info.setVersion("1.0.0");
        openAPIWithEmptyPaths.setInfo(info);

        // Act
        dynamicMcpToolsGenerator.loadOpenApiSpec(openAPIWithEmptyPaths);

        // Assert
        assertThat(dynamicMcpToolsGenerator.getToolDefinitionsCount()).isEqualTo(0);
    }

    @Test
    void testExecuteOpenApiOperation_OperationNotFound() {
        // Arrange
        String nonExistentOperationId = "nonExistentOperation";

        // Act
        HttpResponseBean response = dynamicMcpToolsGenerator.executeOpenApiOperation(
                nonExistentOperationId, null, null, null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("操作未找到");
    }

    @Test
    void testExecuteOpenApiOperation_Success() {
        // Arrange
        OpenAPI mockOpenAPI = createDetailedMockOpenAPI();
        dynamicMcpToolsGenerator.loadOpenApiSpec(mockOpenAPI);

        HttpResponseBean mockResponse = new HttpResponseBean();
        mockResponse.setStatusCode(200);
        mockResponse.setBody("{\"success\": true}");

        when(producerTemplate.requestBody(eq("direct:httpRequest"), any(), eq(HttpResponseBean.class)))
                .thenReturn(mockResponse);

        // Act
        HttpResponseBean response = dynamicMcpToolsGenerator.executeOpenApiOperation(
                "getPet", null, "{\"petId\": \"123\"}", null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("success");
    }

    @Test
    void testListAvailableTools() {
        // Arrange
        OpenAPI mockOpenAPI = createDetailedMockOpenAPI();
        dynamicMcpToolsGenerator.loadOpenApiSpec(mockOpenAPI);

        // Act
        Map<String, String> availableTools = dynamicMcpToolsGenerator.listAvailableTools();

        // Assert
        assertThat(availableTools).isNotEmpty();
        assertThat(availableTools).containsKey("getPet");
        assertThat(availableTools.get("getPet")).contains("GET");
        assertThat(availableTools.get("getPet")).contains("/pets/{petId}");
    }

    @Test
    void testGetToolDetails_Success() {
        // Arrange
        OpenAPI mockOpenAPI = createDetailedMockOpenAPI();
        dynamicMcpToolsGenerator.loadOpenApiSpec(mockOpenAPI);

        // Act
        Map<String, Object> toolDetails = dynamicMcpToolsGenerator.getToolDetails("getPet");

        // Assert
        assertThat(toolDetails).isNotEmpty();
        assertThat(toolDetails.get("operationId")).isEqualTo("getPet");
        assertThat(toolDetails.get("method")).isEqualTo("GET");
        assertThat(toolDetails.get("path")).isEqualTo("/pets/{petId}");
        assertThat(toolDetails.get("hasRequestBody")).isEqualTo(false);
        assertThat(toolDetails).containsKey("parameters");
    }

    @Test
    void testGetToolDetails_OperationNotFound() {
        // Arrange
        String nonExistentOperationId = "nonExistentOperation";

        // Act
        Map<String, Object> toolDetails = dynamicMcpToolsGenerator.getToolDetails(nonExistentOperationId);

        // Assert
        assertThat(toolDetails).containsKey("error");
        assertThat(toolDetails.get("error")).toString().contains("操作未找到");
    }

    @Test
    void testClearToolDefinitions() {
        // Arrange
        OpenAPI mockOpenAPI = createDetailedMockOpenAPI();
        dynamicMcpToolsGenerator.loadOpenApiSpec(mockOpenAPI);
        assertThat(dynamicMcpToolsGenerator.getToolDefinitionsCount()).isGreaterThan(0);

        // Act
        dynamicMcpToolsGenerator.clearToolDefinitions();

        // Assert
        assertThat(dynamicMcpToolsGenerator.getToolDefinitionsCount()).isEqualTo(0);
        assertThat(dynamicMcpToolsGenerator.getCurrentOpenAPI()).isNull();
    }

    /**
     * 创建详细的模拟OpenAPI对象用于测试
     */
    private OpenAPI createDetailedMockOpenAPI() {
        OpenAPI openAPI = new OpenAPI();

        // 设置基本信息
        io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();
        info.setTitle("Pet Store API");
        info.setVersion("1.0.0");
        info.setDescription("A sample Pet Store API for testing");
        openAPI.setInfo(info);

        // 设置服务器
        io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
        server.setUrl("https://petstore.swagger.io/v2");
        openAPI.addServersItem(server);

        // 设置路径
        io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();

        // 创建 GET /pets/{petId} 操作
        io.swagger.v3.oas.models.PathItem petPathItem = new io.swagger.v3.oas.models.PathItem();
        io.swagger.v3.oas.models.Operation getPetOperation = new io.swagger.v3.oas.models.Operation();
        getPetOperation.setOperationId("getPet");
        getPetOperation.setSummary("根据ID获取宠物");
        getPetOperation.setDescription("通过宠物ID获取宠物详细信息");

        // 添加路径参数
        io.swagger.v3.oas.models.parameters.Parameter petIdParam = new io.swagger.v3.oas.models.parameters.PathParameter();
        petIdParam.setName("petId");
        petIdParam.setDescription("宠物ID");
        petIdParam.setRequired(true);
        
        io.swagger.v3.oas.models.media.Schema<Long> petIdSchema = new io.swagger.v3.oas.models.media.IntegerSchema();
        petIdSchema.setFormat("int64");
        petIdParam.setSchema(petIdSchema);
        
        getPetOperation.addParametersItem(petIdParam);

        // 添加响应
        io.swagger.v3.oas.models.responses.ApiResponses responses = new io.swagger.v3.oas.models.responses.ApiResponses();
        io.swagger.v3.oas.models.responses.ApiResponse successResponse = new io.swagger.v3.oas.models.responses.ApiResponse();
        successResponse.setDescription("成功获取宠物信息");
        responses.addApiResponse("200", successResponse);
        getPetOperation.setResponses(responses);

        petPathItem.setGet(getPetOperation);
        paths.addPathItem("/pets/{petId}", petPathItem);

        // 创建 POST /pets 操作
        io.swagger.v3.oas.models.PathItem petsPathItem = new io.swagger.v3.oas.models.PathItem();
        io.swagger.v3.oas.models.Operation createPetOperation = new io.swagger.v3.oas.models.Operation();
        createPetOperation.setOperationId("createPet");
        createPetOperation.setSummary("创建新宠物");
        createPetOperation.setDescription("在宠物店中创建新的宠物");

        // 添加请求体
        io.swagger.v3.oas.models.parameters.RequestBody requestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
        requestBody.setDescription("宠物数据");
        requestBody.setRequired(true);
        
        io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
        io.swagger.v3.oas.models.media.MediaType jsonMediaType = new io.swagger.v3.oas.models.media.MediaType();
        
        io.swagger.v3.oas.models.media.Schema<Object> petSchema = new io.swagger.v3.oas.models.media.ObjectSchema();
        jsonMediaType.setSchema(petSchema);
        content.addMediaType("application/json", jsonMediaType);
        requestBody.setContent(content);
        
        createPetOperation.setRequestBody(requestBody);
        petsPathItem.setPost(createPetOperation);
        paths.addPathItem("/pets", petsPathItem);

        openAPI.setPaths(paths);

        return openAPI;
    }
}