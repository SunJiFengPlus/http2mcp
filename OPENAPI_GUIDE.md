# OpenAPI 自动测试功能指南

本项目新增了强大的OpenAPI文档解析和自动测试功能，可以从OpenAPI/Swagger规范自动生成并执行API测试用例。

## 功能特性

- 📖 **OpenAPI文档解析**: 支持从URL、文件或字符串内容解析OpenAPI 3.0规范
- 🧪 **自动测试生成**: 基于OpenAPI规范自动生成测试用例
- 🚀 **测试执行**: 自动执行生成的测试用例并提供详细报告
- 📊 **测试报告**: 提供成功率、错误详情等完整的测试摘要
- 🔧 **Spring AI MCP集成**: 通过MCP工具提供AI助手集成

## API 接口

### 1. 从URL解析OpenAPI文档并生成测试用例

```bash
curl -X POST http://localhost:8888/api/openapi/parse-from-url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://petstore3.swagger.io/api/v3/openapi.json"}'
```

### 2. 从文件内容解析OpenAPI文档

```bash
curl -X POST http://localhost:8888/api/openapi/parse-from-content \
  -H "Content-Type: application/json" \
  -d '{"content": "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0\npaths:\n  /test:\n    get:\n      responses:\n        200:\n          description: OK"}'
```

### 3. 从文件上传解析OpenAPI文档

```bash
curl -X POST http://localhost:8888/api/openapi/parse-from-file \
  -F "file=@openapi.yaml"
```

### 4. 执行单个测试用例

```bash
curl -X POST http://localhost:8888/api/openapi/execute-test \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test_get_request",
    "path": "https://httpbin.org/get",
    "method": "GET",
    "expectedResponses": [{
      "statusCode": 200,
      "contentType": "application/json"
    }]
  }'
```

### 5. 批量执行测试用例

```bash
curl -X POST http://localhost:8888/api/openapi/execute-tests \
  -H "Content-Type: application/json" \
  -d '[{
    "name": "test1",
    "path": "https://httpbin.org/get",
    "method": "GET",
    "expectedResponses": [{"statusCode": 200}]
  }, {
    "name": "test2",
    "path": "https://httpbin.org/post",
    "method": "POST",
    "requestBody": "{\"test\": \"data\"}",
    "contentType": "application/json",
    "expectedResponses": [{"statusCode": 200}]
  }]'
```

### 6. 一站式服务：解析并执行测试

```bash
curl -X POST http://localhost:8888/api/openapi/parse-and-test \
  -H "Content-Type: application/json" \
  -d '{"url": "https://petstore3.swagger.io/api/v3/openapi.json"}'
```

## Spring AI MCP工具

本项目集成了Spring AI MCP功能，提供了以下工具：

### 1. 解析OpenAPI文档工具

```json
{
  "tool": "parseOpenApiFromUrl",
  "parameters": {
    "url": "https://petstore3.swagger.io/api/v3/openapi.json"
  }
}
```

### 2. 执行OpenAPI测试工具

```json
{
  "tool": "executeOpenApiTests",
  "parameters": {
    "url": "https://petstore3.swagger.io/api/v3/openapi.json"
  }
}
```

## 测试用例自动生成规则

系统会根据OpenAPI规范自动生成测试用例，包括：

### 参数处理
- **路径参数**: 从schema或example中提取示例值，替换URL中的占位符
- **查询参数**: 自动添加到请求的查询字符串中
- **请求头**: 添加到HTTP请求头中
- **请求体**: 根据schema生成JSON格式的示例数据

### 响应验证
- **状态码验证**: 检查响应状态码是否符合OpenAPI定义
- **Content-Type验证**: 验证响应的媒体类型
- **响应体模式**: 支持精确匹配和正则表达式匹配
- **响应头验证**: 检查期望的响应头是否存在

### 示例值生成策略

系统按以下优先级生成示例值：
1. OpenAPI规范中的`example`字段
2. Schema中的`example`字段
3. 根据数据类型自动生成：
   - `string`: 根据format生成（email、date、uuid等）
   - `integer/number`: 生成数字123
   - `boolean`: 生成true
   - `array`: 生成包含单个示例元素的数组
   - `object`: 递归生成所有属性的示例

## 使用示例

### 示例1: 测试HTTPBin API

```yaml
# httpbin-test.yaml
openapi: 3.0.0
info:
  title: HTTPBin Test
  version: 1.0.0
servers:
  - url: https://httpbin.org
paths:
  /get:
    get:
      parameters:
        - name: test_param
          in: query
          schema:
            type: string
            example: hello
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                type: object
  /post:
    post:
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                  example: John
                age:
                  type: integer
                  example: 30
      responses:
        '200':
          description: Success
```

使用curl测试：
```bash
# 解析并执行测试
curl -X POST http://localhost:8888/api/openapi/parse-and-test \
  -H "Content-Type: application/json" \
  -d '{"url": "file://./httpbin-test.yaml"}'
```

### 示例2: 自定义测试验证

```json
{
  "name": "custom_validation_test",
  "path": "https://httpbin.org/json",
  "method": "GET",
  "expectedResponses": [{
    "statusCode": 200,
    "contentType": "application/json",
    "expectedBodyPattern": "regex:.*slideshow.*",
    "expectedHeaders": {
      "Content-Type": "application/json"
    }
  }]
}
```

## 配置

在`application.yml`中可以配置相关参数：

```yaml
# 应用配置
server:
  port: 8888

spring:
  application:
    name: camel-mcp-demo

# Camel配置
camel:
  springboot:
    main-run-controller: true
```

## 运行测试

### 运行集成测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=OpenApiIntegrationTest
```

### 启动应用
```bash
mvn spring-boot:run
```

## 测试报告示例

执行测试后，系统会返回详细的测试报告：

```json
{
  "totalTests": 5,
  "successfulTests": 4,
  "failedTests": 1,
  "totalDuration": 2345,
  "successRate": 80.0,
  "results": [
    {
      "testCaseName": "getPets_test",
      "success": true,
      "executionTime": "2023-12-25T10:30:00",
      "durationMs": 456,
      "actualResponse": {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": "[{\"id\":1,\"name\":\"Fluffy\"}]"
      },
      "errors": [],
      "warnings": []
    }
  ]
}
```

## 最佳实践

1. **使用完整的OpenAPI规范**: 包含详细的参数、响应和示例，有助于生成更准确的测试用例
2. **提供示例值**: 在OpenAPI规范中提供`example`字段，确保测试使用有意义的数据
3. **定义完整的响应**: 包括所有可能的状态码和响应格式
4. **使用标准HTTP状态码**: 遵循REST API设计最佳实践
5. **测试环境准备**: 确保目标API服务在执行测试时可访问

## 故障排除

### 常见问题

1. **解析失败**: 检查OpenAPI文档格式是否正确（YAML或JSON）
2. **网络连接**: 确保能访问OpenAPI文档URL和目标API服务
3. **认证问题**: 如果API需要认证，在测试用例中添加相应的headers
4. **超时问题**: 检查网络连接和API响应时间

### 日志查看

应用使用标准的Spring Boot日志配置，可以通过以下方式查看详细日志：

```bash
# 启动时启用调试日志
java -jar target/main-1.0.0.jar --logging.level.org.apache.camel.examples=DEBUG
```

## 扩展开发

如需扩展功能，可以：

1. **自定义测试验证器**: 继承`OpenApiTestService`并添加自定义验证逻辑
2. **添加新的参数类型支持**: 在`OpenApiParserService`中扩展参数处理逻辑
3. **集成其他测试框架**: 将生成的测试用例导出到其他测试框架格式
4. **添加性能测试**: 扩展测试执行器以支持并发和性能测试

## 许可证

本项目遵循Apache License 2.0许可证。