package org.apache.camel.examples.controller;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.*;
import org.apache.camel.examples.service.McpToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api")
public class HttpRequestController {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @Autowired
    private McpToolService mcpToolService;
    
    /**
     * 原有的HTTP请求接口 - 保持不变
     */
    @PostMapping("/http/request")
    public ResponseEntity<HttpResponseBean> sendHttpRequest(@RequestBody HttpRequestBean requestBean) {
        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
        
        // 根据HTTP响应状态码设置Spring响应状态
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
    
    /**
     * MCP工具执行接口
     */
    @PostMapping("/mcp/tools")
    public ResponseEntity<McpToolResponse> executeMcpTool(@RequestBody McpToolRequest request) {
        McpToolResponse response = mcpToolService.executeTool(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 列出所有可用的MCP工具
     */
    @GetMapping("/mcp/tools/list")
    public ResponseEntity<List<McpToolDefinition>> listMcpTools() {
        List<McpToolDefinition> tools = mcpToolService.getAvailableTools();
        return ResponseEntity.ok(tools);
    }
    
    /**
     * 获取特定工具的详细信息
     */
    @GetMapping("/mcp/tools/{toolName}")
    public ResponseEntity<McpToolDefinition> getMcpToolInfo(@PathVariable String toolName) {
        return mcpToolService.getAvailableTools()
            .stream()
            .filter(tool -> tool.getName().equals(toolName))
            .findFirst()
            .map(tool -> ResponseEntity.ok(tool))
            .orElse(ResponseEntity.notFound().build());
    }
}
