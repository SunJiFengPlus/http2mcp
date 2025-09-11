package org.apache.camel.examples.openapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.examples.openapi.service.OpenApiIndividualToolGenerator;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OpenAPI MCP配置类
 * 负责合并传统工具和基于OpenAPI生成的独立MCP工具
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenApiMcpConfig {
    
    private final ResourceLoader resourceLoader;
    private final OpenApiIndividualToolGenerator openApiIndividualToolGenerator;
    
    @Value("${openapi.config.file:}")
    private String openApiConfigFile;
    
    @Value("${openapi.config.enabled:true}")
    private boolean openApiConfigEnabled;
    
    /**
     * 创建合并的工具回调提供者（传统工具 + 每个OpenAPI接口作为独立工具）
     */
    @Bean
    public ToolCallbackProvider mergedToolCallbackProvider(
            @Qualifier("traditionalToolObjects") List<Object> traditionalTools) {
        
        log.info("正在初始化合并的MCP工具提供者...");
        
        List<Object> allTools = new ArrayList<>();
        
        // 添加传统工具
        allTools.addAll(traditionalTools);
        log.info("已包含 {} 个传统@Tool工具", traditionalTools.size());
        
        // 添加OpenAPI工具
        if (openApiConfigEnabled && openApiConfigFile != null && !openApiConfigFile.trim().isEmpty()) {
            Optional<OpenAPI> openApiOptional = loadOpenApiSpec(openApiConfigFile);
            
            if (openApiOptional.isPresent()) {
                OpenAPI openApi = openApiOptional.get();
                log.info("成功加载OpenAPI配置: {} v{}", 
                    openApi.getInfo() != null ? openApi.getInfo().getTitle() : "Unknown API",
                    openApi.getInfo() != null ? openApi.getInfo().getVersion() : "Unknown"
                );
                
                // 为每个API接口创建独立的工具对象
                List<Object> openApiTools = openApiIndividualToolGenerator.createIndividualTools(openApi);
                allTools.addAll(openApiTools);
                
                int operationCount = openApi.getPaths() != null ? 
                    openApi.getPaths().values().stream()
                        .mapToInt(this::countOperations)
                        .sum() : 0;
                log.info("为 {} 个OpenAPI操作创建了独立的工具", operationCount);
                
            } else {
                log.warn("无法加载OpenAPI配置文件: {}", openApiConfigFile);
            }
        } else {
            log.info("OpenAPI配置未启用或未指定文件");
        }
        
        // 创建包含所有工具的统一提供者
        log.info("创建包含传统工具和OpenAPI独立工具的合并提供者，总计 {} 个工具", allTools.size());
        
        return MethodToolCallbackProvider.builder()
            .toolObjects(allTools.toArray())
            .build();
    }
    
    private int countOperations(io.swagger.v3.oas.models.PathItem pathItem) {
        int count = 0;
        if (pathItem.getGet() != null) count++;
        if (pathItem.getPost() != null) count++;
        if (pathItem.getPut() != null) count++;
        if (pathItem.getDelete() != null) count++;
        if (pathItem.getPatch() != null) count++;
        return count;
    }
    
    /**
     * 加载OpenAPI规范
     */
    private Optional<OpenAPI> loadOpenApiSpec(String configFile) {
        try {
            Resource resource = resourceLoader.getResource(configFile);
            
            if (!resource.exists()) {
                log.warn("OpenAPI配置文件不存在: {}", configFile);
                return Optional.empty();
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content, null, null).getOpenAPI();
            
            if (openAPI == null) {
                log.warn("无法解析OpenAPI内容");
                return Optional.empty();
            }
            
            return Optional.of(openAPI);
            
        } catch (Exception e) {
            log.error("加载OpenAPI配置文件失败: {}", configFile, e);
            return Optional.empty();
        }
    }
}