# OpenAPI到MCP工具转换指南

本文档介绍如何使用新创建的服务类将OpenAPI规范转换为MCP（Model Context Protocol）工具。

## 概述

我们创建了三个主要的服务类来实现OpenAPI到MCP工具的转换：

1. **`OpenApiToMcpToolsService`** - 基础转换服务，提供静态工具注册
2. **`DynamicMcpToolsGenerator`** - 动态工具生成器，提供通用的工具执行能力
3. **`OpenApiMcpToolsManager`** - 工具管理器，负责协调和管理整个生命周期

## 功能特性

### 🔧 核心功能

- ✅ 从OpenAPI 3.x规范文件加载工具定义
- ✅ 支持YAML和JSON格式的OpenAPI文件
- ✅ 自动提取路径参数、查询参数和请求体
- ✅ 动态生成MCP工具注解
- ✅ 集成现有的HTTP路由基础设施
- ✅ 提供工具验证和错误处理

### 🚀 高级特性

- ✅ 支持批量加载多个OpenAPI文件
- ✅ 提供通用工具执行接口
- ✅ 工具列表和详情查询功能
- ✅ 配置化的自动加载机制
- ✅ 完整的错误处理和日志记录

## 使用方法

### 1. 配置文件设置

在 `application.yml` 中配置OpenAPI工具：

```yaml
openapi:
  # 默认的OpenAPI规范文件路径
  default-spec-path: src/test/resources/sample-openapi.yaml
  # 自动加载OpenAPI规范文件的目录
  auto-load-directory: src/test/resources
  # 是否在应用启动时自动加载OpenAPI规范
  auto-load-on-startup: true
  # 工具配置
  tools:
    enable-static-tools: true
    enable-dynamic-tools: true
    tool-name-prefix: "openapi_"
    default-timeout: 30000
```

### 2. 手动加载OpenAPI规范

#### 使用管理器服务
```java
@Autowired
private OpenApiMcpToolsManager toolsManager;

// 从文件加载
String result = toolsManager.loadOpenApiSpec("/path/to/openapi.yaml");

// 从目录批量加载
String result = toolsManager.loadOpenApiSpecsFromDirectory("/path/to/openapi/specs");
```

#### 使用转换服务
```java
@Autowired
private OpenApiToMcpToolsService conversionService;

// 直接注册OpenAPI对象
conversionService.registerToolsFromOpenApi(openApiObject);

// 从文件注册
conversionService.registerToolsFromOpenApiFile("/path/to/spec.yaml");
```

### 3. 使用MCP工具

#### 通用工具执行
```java
@Autowired
private DynamicMcpToolsGenerator generator;

// 执行任何已注册的OpenAPI操作
HttpResponseBean response = generator.executeOpenApiOperation(
    "getPetById",           // operationId
    "{\"limit\": 10}",     // 查询参数(JSON)
    "{\"petId\": \"123\"}", // 路径参数(JSON)
    "{\"Authorization\": \"Bearer token\"}", // 请求头(JSON)
    "{\"name\": \"Buddy\"}" // 请求体(JSON)
);
```

#### 获取工具信息
```java
// 列出所有可用工具
Map<String, String> tools = generator.listAvailableTools();

// 获取特定工具的详细信息
Map<String, Object> details = generator.getToolDetails("createPet");

// 获取工具概览
Map<String, Object> overview = toolsManager.getToolsOverview();
```

## API参考

### OpenApiMcpToolsManager

**主要方法：**
- `loadOpenApiSpec(String filePath)` - 从文件加载OpenAPI规范
- `loadOpenApiSpecsFromDirectory(String directory)` - 批量加载目录中的规范文件
- `getToolsOverview()` - 获取当前工具概览
- `reloadAllTools()` - 重新加载所有工具
- `validateOpenApiSpec(String filePath)` - 验证OpenAPI规范

**MCP工具注解：**
```java
@Tool(description = "从文件加载OpenAPI规范并生成MCP工具")
public String loadOpenApiSpec(@ToolParam(description = "OpenAPI规范文件路径") String filePath)
```

### DynamicMcpToolsGenerator

**主要方法：**
- `loadOpenApiSpec(OpenAPI openAPI)` - 加载OpenAPI规范
- `executeOpenApiOperation(...)` - 执行OpenAPI操作
- `listAvailableTools()` - 列出可用工具
- `getToolDetails(String operationId)` - 获取工具详情

**MCP工具注解：**
```java
@Tool(description = "执行OpenAPI操作的通用工具")
public HttpResponseBean executeOpenApiOperation(
    @ToolParam(description = "操作ID") String operationId,
    @ToolParam(description = "查询参数，JSON字符串格式", required = false) String queryParams,
    @ToolParam(description = "路径参数，JSON字符串格式", required = false) String pathParams,
    @ToolParam(description = "请求头，JSON字符串格式", required = false) String headers,
    @ToolParam(description = "请求体，JSON字符串格式", required = false) String requestBody
)
```

### OpenApiToMcpToolsService

**主要方法：**
- `registerToolsFromOpenApi(OpenAPI openAPI)` - 注册OpenAPI工具
- `registerToolsFromOpenApiFile(String filePath)` - 从文件注册工具
- `getRegisteredTools()` - 获取已注册的工具
- `clearRegisteredTools()` - 清空工具注册

## 示例OpenAPI规范

项目中包含了一个示例OpenAPI规范文件 `src/test/resources/sample-openapi.yaml`，展示了：

- 多种HTTP方法 (GET, POST, PUT, DELETE)
- 路径参数和查询参数
- 请求体和响应模式
- 参数验证和类型定义

## 错误处理

系统提供了完整的错误处理机制：

1. **文件读取错误** - 文件不存在或权限问题
2. **解析错误** - 无效的OpenAPI格式
3. **验证错误** - 不符合OpenAPI规范
4. **执行错误** - HTTP请求执行失败

所有错误都会记录到日志，并返回友好的错误信息。

## 日志配置

```yaml
logging:
  level:
    org.apache.camel.examples.service: DEBUG
    org.springframework.ai: INFO
```

## 测试

项目包含了完整的单元测试：

- `OpenApiToMcpToolsServiceTest` - 转换服务测试
- `DynamicMcpToolsGeneratorTest` - 动态生成器测试

## 最佳实践

1. **文件组织** - 将OpenAPI规范文件放在 `resources` 目录下
2. **命名规范** - 确保 `operationId` 唯一且有意义
3. **参数文档** - 为所有参数提供清晰的描述
4. **错误处理** - 在OpenAPI中定义完整的错误响应
5. **版本控制** - 为API版本更新制定策略

## 扩展可能

- 支持OpenAPI 3.1规范
- 添加认证和授权处理
- 实现响应数据转换和映射
- 集成API文档生成
- 支持批量操作

## 故障排除

### 常见问题

1. **工具未注册** - 检查 `operationId` 是否正确设置
2. **参数类型错误** - 验证JSON参数格式
3. **HTTP请求失败** - 检查目标服务器状态
4. **配置未生效** - 确认 `application.yml` 路径正确

### 调试建议

启用DEBUG日志查看详细信息：
```yaml
logging:
  level:
    org.apache.camel.examples.service: DEBUG
```

通过管理端点监控工具状态：
```
GET /actuator/health
GET /actuator/metrics
```