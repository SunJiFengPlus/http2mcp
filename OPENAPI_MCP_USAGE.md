# OpenAPI 转 MCP 使用指南

本项目已成功实现了从 OpenAPI 3.1 文件读取配置并自动生成 MCP（Model Context Protocol）工具的功能。

## 功能特性

✅ **完整的 OpenAPI 3.1 支持**
- 支持 YAML 和 JSON 格式的 OpenAPI 文件
- 解析路径、参数、请求体、响应等完整配置
- 自动提取服务器 URL 作为基础地址

✅ **动态 MCP 工具生成**
- 基于 OpenAPI 操作自动生成对应的 MCP 工具
- 支持 GET、POST、PUT、DELETE、PATCH 等 HTTP 方法
- 自动处理路径参数、查询参数、请求头和请求体

✅ **企业级特性**
- 完整的错误处理和日志记录
- 支持复杂参数映射和数据转换
- 可扩展的架构设计

## 快速开始

### 1. 配置 OpenAPI 文件路径

在 `application.yml` 中配置你的 OpenAPI 文件路径：

```yaml
openapi:
  config:
    enabled: true
    # 支持 classpath: 和 file: 前缀
    file: "classpath:openapi/your-api.yaml"
    # 或者
    # file: "file:/path/to/your/api.yaml"
```

### 2. 创建 OpenAPI 文件

创建你的 OpenAPI 3.1 配置文件，例如 `src/main/resources/openapi/your-api.yaml`：

```yaml
openapi: 3.1.0
info:
  title: 我的API
  version: 1.0.0
  description: API描述
servers:
  - url: https://api.example.com
    description: 生产环境
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
      responses:
        '200':
          description: 成功
  /users:
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
                email:
                  type: string
      responses:
        '201':
          description: 创建成功
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

或者使用环境变量：

```bash
OPENAPI_CONFIG_FILE=classpath:openapi/your-api.yaml mvn spring-boot:run
```

### 4. 使用生成的 MCP 工具

应用启动后，系统会自动：
1. 解析 OpenAPI 文件
2. 为每个操作生成对应的 MCP 工具
3. 注册工具到 Spring AI MCP 服务器

你将在日志中看到类似输出：
```
INFO  - 开始生成基于OpenAPI配置的动态MCP工具
INFO  - 找到 2 个OpenAPI操作，开始生成动态工具
INFO  - 成功生成 2 个动态MCP工具:
INFO  - - getUserById (GET /users/{id}): 根据ID获取用户
INFO  - - createUser (POST /users): 创建用户
```

## 示例配置文件

项目包含了一个完整的示例配置文件 `src/main/resources/openapi/example-api.yaml`，演示了：

- **测试 GET 请求**：`testGet` - 向 httpbin 发送 GET 请求
- **测试 POST 请求**：`testPost` - 向 httpbin 发送 POST 请求  
- **测试状态码**：`testHttpStatus` - 返回指定的 HTTP 状态码
- **测试延迟响应**：`testDelay` - 延迟响应测试
- **测试请求头**：`testHeaders` - 返回请求头信息

### 使用示例配置

```bash
OPENAPI_CONFIG_FILE=classpath:openapi/example-api.yaml mvn spring-boot:run
```

## 支持的参数类型

### 路径参数 (Path Parameters)
```yaml
parameters:
  - name: userId
    in: path
    required: true
    schema:
      type: integer
```

### 查询参数 (Query Parameters)  
```yaml
parameters:
  - name: limit
    in: query
    schema:
      type: integer
      default: 10
```

### 请求头 (Headers)
```yaml
parameters:
  - name: Authorization
    in: header
    schema:
      type: string
```

### 请求体 (Request Body)
```yaml
requestBody:
  required: true
  content:
    application/json:
      schema:
        type: object
        properties:
          name:
            type: string
```

## 配置选项

| 配置项 | 描述 | 默认值 | 示例 |
|--------|------|--------|------|
| `openapi.config.enabled` | 是否启用 OpenAPI MCP 功能 | `true` | `true`/`false` |
| `openapi.config.file` | OpenAPI 文件路径 | 空 | `classpath:openapi/api.yaml` |

## 测试

运行测试验证功能：

```bash
mvn test
```

测试覆盖了：
- OpenAPI 文件解析（包括复杂参数和错误处理）
- MCP 工具生成和执行
- HTTP 请求映射和响应处理
- 集成测试场景

## 扩展功能

### 自定义参数映射

你可以通过继承或配置来自定义参数映射逻辑，支持：
- 数据转换和验证
- 认证信息注入  
- 响应格式化
- 错误处理策略

### 添加中间件

项目支持添加各种中间件来处理：
- 认证鉴权
- 日志记录
- 数据映射
- 缓存策略

## 故障排除

### 常见问题

1. **OpenAPI 文件未找到**
   - 确认文件路径正确
   - 检查 classpath 或文件系统路径

2. **解析失败**
   - 验证 OpenAPI 文件语法
   - 确认符合 OpenAPI 3.1 规范

3. **工具未生成**
   - 检查 operationId 是否唯一
   - 确认路径配置正确

### 日志级别

设置日志级别获取详细信息：

```yaml
logging:
  level:
    org.apache.camel.examples.openapi: DEBUG
```

## 下一步

1. ✅ 基本 OpenAPI 解析和 MCP 工具生成 
2. 🚧 实现数据映射和转换功能
3. 🚧 添加认证鉴权支持
4. 🚧 实现 SpringBoot Starter
5. 🚧 添加更多预制处理逻辑

---

现在你可以轻松地将任何现有的 REST API 转换为 MCP 协议，让 AI 模型能够直接调用你的 API！