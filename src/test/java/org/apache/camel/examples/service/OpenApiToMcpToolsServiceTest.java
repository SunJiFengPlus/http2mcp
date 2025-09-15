package org.apache.camel.examples.service;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * OpenAPI到MCP工具转换服务的测试
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
public class OpenApiToMcpToolsServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private OpenApiParserService openApiParserService;

    private OpenApiToMcpToolsService openApiToMcpToolsService;

    @BeforeEach
    void setUp() {
        openApiToMcpToolsService = new OpenApiToMcpToolsService(producerTemplate, openApiParserService);
    }

    @Test
    void testRegisterToolsFromOpenApiFile_Success() throws Exception {
        // Arrange
        String filePath = "test-openapi.yaml";
        OpenAPI mockOpenAPI = createMockOpenAPI();
        
        when(openApiParserService.parseFromFile(filePath)).thenReturn(mockOpenAPI);
        when(openApiParserService.isValidOpenAPI(mockOpenAPI)).thenReturn(true);

        // Act
        openApiToMcpToolsService.registerToolsFromOpenApiFile(filePath);

        // Assert
        verify(openApiParserService).parseFromFile(filePath);
        verify(openApiParserService).isValidOpenAPI(mockOpenAPI);
        
        // 验证工具已注册（基于mock OpenAPI的内容）
        var registeredTools = openApiToMcpToolsService.getRegisteredTools();
        assertThat(registeredTools).isNotEmpty();
    }

    @Test
    void testRegisterToolsFromOpenApiFile_FileNotFound() throws Exception {
        // Arrange
        String filePath = "non-existent-file.yaml";
        when(openApiParserService.parseFromFile(filePath))
            .thenThrow(new RuntimeException("文件未找到"));

        // Act & Assert
        assertThatThrownBy(() -> openApiToMcpToolsService.registerToolsFromOpenApiFile(filePath))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("注册OpenAPI工具失败");
    }

    @Test
    void testRegisterToolsFromOpenApi_InvalidOpenAPI() {
        // Arrange
        OpenAPI invalidOpenAPI = new OpenAPI();
        when(openApiParserService.isValidOpenAPI(invalidOpenAPI)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> openApiToMcpToolsService.registerToolsFromOpenApi(invalidOpenAPI))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("无效的OpenAPI规范");
    }

    @Test
    void testRegisterToolsFromOpenApi_NullPaths() {
        // Arrange
        OpenAPI openAPIWithNullPaths = new OpenAPI();
        when(openApiParserService.isValidOpenAPI(openAPIWithNullPaths)).thenReturn(true);

        // Act
        openApiToMcpToolsService.registerToolsFromOpenApi(openAPIWithNullPaths);

        // Assert
        var registeredTools = openApiToMcpToolsService.getRegisteredTools();
        assertThat(registeredTools).isEmpty();
    }

    @Test
    void testClearRegisteredTools() {
        // Arrange
        OpenAPI mockOpenAPI = createMockOpenAPI();
        when(openApiParserService.isValidOpenAPI(mockOpenAPI)).thenReturn(true);
        openApiToMcpToolsService.registerToolsFromOpenApi(mockOpenAPI);

        // Verify tools are registered
        assertThat(openApiToMcpToolsService.getRegisteredTools()).isNotEmpty();

        // Act
        openApiToMcpToolsService.clearRegisteredTools();

        // Assert
        assertThat(openApiToMcpToolsService.getRegisteredTools()).isEmpty();
    }

    @Test
    void testGetRegisteredTools_ReturnsImmutableCopy() {
        // Arrange
        OpenAPI mockOpenAPI = createMockOpenAPI();
        when(openApiParserService.isValidOpenAPI(mockOpenAPI)).thenReturn(true);
        openApiToMcpToolsService.registerToolsFromOpenApi(mockOpenAPI);

        // Act
        var tools1 = openApiToMcpToolsService.getRegisteredTools();
        var tools2 = openApiToMcpToolsService.getRegisteredTools();

        // Assert
        assertThat(tools1).isNotSameAs(tools2); // 应该返回不同的实例（防御性复制）
        assertThat(tools1).isEqualTo(tools2); // 但内容应该相同
    }

    /**
     * 创建用于测试的模拟OpenAPI对象
     */
    private OpenAPI createMockOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        
        // 设置基本信息
        io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        info.setDescription("Test OpenAPI specification");
        openAPI.setInfo(info);

        // 设置服务器
        io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
        server.setUrl("https://api.example.com");
        openAPI.addServersItem(server);

        // 设置路径
        io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
        
        // 创建一个简单的GET操作
        io.swagger.v3.oas.models.PathItem pathItem = new io.swagger.v3.oas.models.PathItem();
        io.swagger.v3.oas.models.Operation operation = new io.swagger.v3.oas.models.Operation();
        operation.setOperationId("getTest");
        operation.setSummary("测试操作");
        operation.setDescription("这是一个测试操作");
        
        // 添加一个查询参数
        io.swagger.v3.oas.models.parameters.Parameter parameter = new io.swagger.v3.oas.models.parameters.QueryParameter();
        parameter.setName("testParam");
        parameter.setDescription("测试参数");
        parameter.setRequired(false);
        
        io.swagger.v3.oas.models.media.Schema<String> paramSchema = new io.swagger.v3.oas.models.media.StringSchema();
        parameter.setSchema(paramSchema);
        
        operation.addParametersItem(parameter);
        pathItem.setGet(operation);
        
        paths.addPathItem("/test", pathItem);
        openAPI.setPaths(paths);

        return openAPI;
    }
}