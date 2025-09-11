# OpenAPI YAML读取器

这是一个简单的Spring Boot应用，提供从resources目录中读取OpenAPI 3.1 YAML文件的功能。

## 功能特性

- 从classpath读取OpenAPI 3.1 YAML规范文件
- 使用swagger-parser解析OpenAPI内容
- 提供REST API查看OpenAPI规范信息
- 支持自定义OpenAPI文件路径

## API接口

### 获取OpenAPI基本信息

**GET** `/api/openapi/info`

可选参数：
- `file`: OpenAPI文件路径（默认为配置文件中的路径）

响应示例：
```json
{
  "filePath": "classpath:openapi/example-api.yaml",
  "success": true,
  "title": "示例HTTP API",
  "version": "1.0.0",
  "description": "这是一个示例API配置...",
  "pathCount": 5,
  "paths": ["/get", "/post", "/status/{code}", "/delay/{seconds}", "/headers"],
  "servers": [
    {
      "url": "https://httpbin.org",
      "description": "httpbin.org测试服务器"
    }
  ]
}
```

### 获取完整OpenAPI规范

**GET** `/api/openapi/spec`

可选参数：
- `file`: OpenAPI文件路径（默认为配置文件中的路径）

返回完整的OpenAPI规范对象。

## 配置

在 `application.yml` 中配置默认的OpenAPI文件路径：

```yaml
openapi:
  file: classpath:openapi/example-api.yaml
```

## 使用示例

### 编程方式使用

```java
@Autowired
private OpenApiYamlReader openApiYamlReader;

public void readOpenApiSpec() {
    Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource("classpath:openapi/my-api.yaml");
    
    if (openAPI.isPresent()) {
        OpenAPI api = openAPI.get();
        System.out.println("API标题: " + api.getInfo().getTitle());
        System.out.println("API版本: " + api.getInfo().getVersion());
        // 处理OpenAPI对象...
    }
}
```

## 运行应用

1. 启动应用：
```bash
./mvnw spring-boot:run
```

2. 应用将在端口8888上启动

3. 查看OpenAPI信息：
```bash
curl http://localhost:8888/api/openapi/info
```

4. 获取完整规范：
```bash
curl http://localhost:8888/api/openapi/spec
```

## OpenAPI文件位置

默认的示例OpenAPI文件位于：`src/main/resources/openapi/example-api.yaml`

你可以添加自己的OpenAPI文件到resources目录，并通过API参数或配置文件指定路径。

## 技术栈

- Java 17
- Spring Boot 3.4.5
- Swagger Parser 2.1.22
- Maven
