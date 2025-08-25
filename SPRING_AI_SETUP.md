# Spring AI MCP Tools 配置说明

根据您的要求，我已经将`HttpRequestController`简化，现在只需要在方法上添加`@Tool`和`@ToolParam`注解即可同时支持HTTP接口和MCP tools功能。

## ✅ 当前实现状态

- ✅ 删除了复杂的MCP服务层和模型类
- ✅ 保持原有HTTP接口不变：`POST /api/http/request`
- ✅ 在`sendHttpRequest`方法上添加了Spring AI注解（目前被注释）
- ✅ 所有原有测试通过（9个测试全部成功）

## 📦 Spring AI 依赖配置

要启用MCP tools功能，您需要配置Spring AI依赖。在`pom.xml`中取消注释Spring AI依赖，并添加Spring的milestone仓库：

```xml
<!-- 在 <repositories> 中添加 -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>

<!-- 在 <dependencies> 中取消注释 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

## 🔧 启用MCP Tools注解

配置好依赖后，在`HttpRequestController.java`中取消注释以下内容：

```java
// 取消注释这些导入
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

// 取消注释方法上的注解
@Tool(description = "发送HTTP请求，支持GET、POST等方法")
@PostMapping("/request")
public ResponseEntity<HttpResponseBean> sendHttpRequest(
    @ToolParam(description = "HTTP请求配置信息") @RequestBody HttpRequestBean requestBean) {
    // ... 方法实现保持不变
}
```

## 🎯 最终效果

配置完成后，`sendHttpRequest`方法将同时支持：

1. **HTTP接口**：`POST /api/http/request` - 保持原有功能不变
2. **MCP Tools**：通过`@Tool`注解自动暴露为AI工具

这正是您要求的简洁实现方案！🚀