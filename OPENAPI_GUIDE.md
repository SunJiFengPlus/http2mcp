# OpenAPI 文档解析功能指南

本项目新增了OpenAPI文档解析能力，提供从不同来源读取和解析OpenAPI 3.0规范的功能。

## 功能特性

- 📖 **OpenAPI文档解析**: 支持从文件或字符串内容解析OpenAPI 3.0规范
- ✅ **文档验证**: 验证OpenAPI文档的有效性
- 🔧 **Swagger原生模型**: 使用io.swagger.v3.oas.models.*下的原生模型，确保兼容性

## 核心服务

### OpenApiParserService

这是核心解析服务，提供以下方法：

- `parseFromFile(String filePath)` - 从文件路径解析OpenAPI文档  
- `parseFromString(String content)` - 从字符串内容解析OpenAPI文档
- `isValidOpenAPI(OpenAPI openAPI)` - 验证OpenAPI文档是否有效

## 使用示例

### 1. 基础用法 - 在代码中使用

```java
@Autowired
private OpenApiParserService openApiParserService;

// 从文件解析
OpenAPI openAPI = openApiParserService.parseFromFile("/path/to/openapi.yaml");

// 从字符串解析
String yamlContent = """
    openapi: 3.0.0
    info:
      title: My API
      version: 1.0.0
    paths:
      /users:
        get:
          responses:
            '200':
              description: Success
    """;
OpenAPI openAPI = openApiParserService.parseFromString(yamlContent);

// 验证文档有效性
boolean isValid = openApiParserService.isValidOpenAPI(openAPI);

// 直接访问OpenAPI对象的信息
System.out.println("API标题: " + openAPI.getInfo().getTitle());
System.out.println("API版本: " + openAPI.getInfo().getVersion());
System.out.println("端点数量: " + openAPI.getPaths().size());
```

### 2. 集成到Spring Boot应用

```java
@RestController
public class MyController {
    
    @Autowired
    private OpenApiParserService openApiParserService;
    
    @PostMapping("/api/analyze-openapi")
    public ResponseEntity<?> analyzeOpenAPI(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            OpenAPI openAPI = openApiParserService.parseFromString(content);
            
            return ResponseEntity.ok(Map.of(
                "valid", openApiParserService.isValidOpenAPI(openAPI),
                "title", openAPI.getInfo().getTitle(),
                "version", openAPI.getInfo().getVersion(),
                "pathCount", openAPI.getPaths() != null ? openAPI.getPaths().size() : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("解析失败: " + e.getMessage());
        }
    }
}
```

## 技术特性

- **完整的OpenAPI 3.0支持**: 支持解析完整的OpenAPI 3.0规范
- **多格式支持**: 同时支持JSON和YAML格式的OpenAPI文档
- **错误处理**: 完善的异常处理，提供清晰的错误信息
- **引用解析**: 自动解析OpenAPI文档中的$ref引用
- **验证功能**: 提供基本的文档有效性验证

## 测试覆盖

本功能包含完整的测试覆盖：

### 集成测试 (OpenApiIntegrationTest)
- 测试YAML和JSON格式解析
- 测试文件解析功能
- 测试各种错误场景处理
- 测试复杂OpenAPI文档解析

### 单元测试 (OpenApiParserServiceTest)
- 测试所有公开方法的各种场景
- 测试边界条件和异常情况
- 测试数据验证逻辑

## 实际应用示例

### 示例1: 解析复杂的OpenAPI文档

```java
@Test
public void parseComplexOpenAPI() {
    String yamlContent = """
        openapi: 3.0.0
        info:
          title: Pet Store API
          version: 1.0.0
          description: A pet store API example
        servers:
          - url: https://petstore.example.com/v1
        paths:
          /pets:
            get:
              responses:
                '200':
                  description: A list of pets
          /pets/{id}:
            get:
              parameters:
                - name: id
                  in: path
                  required: true
                  schema:
                    type: integer
              responses:
                '200':
                  description: Pet details
        components:
          schemas:
            Pet:
              type: object
              properties:
                id:
                  type: integer
                name:
                  type: string
        """;
    
    OpenAPI openAPI = openApiParserService.parseFromString(yamlContent);
    
    // 验证解析结果
    assertTrue(openApiParserService.isValidOpenAPI(openAPI));
    
    // 直接访问OpenAPI对象内容
    assertEquals("Pet Store API", openAPI.getInfo().getTitle());
    assertEquals("1.0.0", openAPI.getInfo().getVersion());
    assertEquals("A pet store API example", openAPI.getInfo().getDescription());
    assertEquals(2, openAPI.getPaths().size());
    assertTrue(openAPI.getPaths().containsKey("/pets"));
    assertTrue(openAPI.getPaths().containsKey("/pets/{id}"));
    
    // 验证组件
    assertNotNull(openAPI.getComponents());
    assertTrue(openAPI.getComponents().getSchemas().containsKey("Pet"));
    
    // 验证服务器信息
    assertEquals(1, openAPI.getServers().size());
    assertEquals("https://petstore.example.com/v1", openAPI.getServers().get(0).getUrl());
}
```

### 示例2: 解析本地OpenAPI文件

```java
@Test
public void parseLocalFile() throws IOException {
    String yamlContent = """
        openapi: 3.0.0
        info:
          title: My Local API
          version: 1.0.0
          description: A local API for testing
        paths:
          /users:
            get:
              responses:
                '200':
                  description: List of users
          /users/{id}:
            get:
              parameters:
                - name: id
                  in: path
                  required: true
                  schema:
                    type: integer
              responses:
                '200':
                  description: User details
                '404':
                  description: User not found
        """;
    
    OpenAPI openAPI = openApiParserService.parseFromString(yamlContent);
    
    // 验证解析结果
    assertEquals("My Local API", openAPI.getInfo().getTitle());
    assertEquals(2, openAPI.getPaths().size());
    assertTrue(openAPI.getPaths().containsKey("/users"));
    assertTrue(openAPI.getPaths().containsKey("/users/{id}"));
}

## 依赖配置

本功能已自动包含以下Maven依赖：

```xml
<!-- OpenAPI parsing dependencies -->
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser</artifactId>
    <version>2.1.16</version>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=OpenApiParserServiceTest
mvn test -Dtest=OpenApiIntegrationTest
```

### 启动应用
```bash
mvn spring-boot:run
```

## 最佳实践

1. **异常处理**: 始终捕获和处理`IllegalArgumentException`、`RuntimeException`和`IOException`
2. **文档验证**: 使用`isValidOpenAPI`验证解析结果
3. **资源管理**: 解析大型文档时注意内存使用

## 故障排除

### 常见问题

1. **解析失败**: 检查OpenAPI文档格式是否正确（YAML或JSON）
2. **文件读取失败**: 检查文件路径和权限
3. **内存不足**: 对于特别大的OpenAPI文档，可能需要调整JVM内存设置

### 异常说明

- `IllegalArgumentException`: 输入参数为空或无效
- `RuntimeException`: OpenAPI文档格式无效或解析失败
- `IOException`: 文件读取失败

### 日志查看

启用调试日志查看详细的解析过程：

```yaml
logging:
  level:
    org.apache.camel.examples.service: DEBUG
    io.swagger.v3.parser: DEBUG
```

## 扩展开发

如需扩展功能，可以：

1. **添加自定义验证**: 扩展`isValidOpenAPI`方法增加自定义验证规则
2. **支持更多格式**: 添加对其他API规范格式的支持
3. **集成缓存**: 为频繁访问的OpenAPI文档添加缓存机制

## 许可证

本项目遵循Apache License 2.0许可证。