#!/bin/bash

echo "🚀 启动 OpenAPI 转 MCP 演示"
echo "================================="
echo

echo "📋 使用示例 OpenAPI 配置文件: example-api.yaml"
echo "🌐 目标服务器: httpbin.org"
echo "🔧 将生成以下 MCP 工具:"
echo "   - testGet: 测试 GET 请求"
echo "   - testPost: 测试 POST 请求"  
echo "   - testHttpStatus: 测试 HTTP 状态码"
echo "   - testDelay: 测试延迟响应"
echo "   - testHeaders: 测试请求头"
echo

echo "▶️  正在启动应用..."
echo

# 设置环境变量使用示例配置
export OPENAPI_CONFIG_FILE=classpath:openapi/example-api.yaml

# 启动 Spring Boot 应用
mvn spring-boot:run -Dspring-boot.run.profiles=example

echo
echo "✅ 应用已启动！查看上方日志确认 MCP 工具生成情况。"
echo "💡 提示: 应用启动后，MCP 工具将自动注册并可供 AI 模型调用。"