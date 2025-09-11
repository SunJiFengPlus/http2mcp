package org.apache.camel.examples.openapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.examples.openapi.model.OpenApiConfig;
import org.apache.camel.examples.openapi.parser.OpenApiParser;
import org.apache.camel.examples.openapi.service.DynamicMcpToolGenerator;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Optional;

/**
 * OpenAPI MCP配置类
 * 负责从OpenAPI文件生成和注册MCP工具
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenApiMcpConfig {
    
    private final OpenApiParser openApiParser;
    private final DynamicMcpToolGenerator dynamicMcpToolGenerator;
    private final ResourceLoader resourceLoader;
    
    @Value("${openapi.config.file:}")
    private String openApiConfigFile;
    
    @Value("${openapi.config.enabled:true}")
    private boolean openApiConfigEnabled;
    
    /**
     * 创建基于OpenAPI的工具回调提供者
     */
    @Bean
    @Primary
    public ToolCallbackProvider openApiToolCallbackProvider() {
        log.info("正在初始化OpenAPI MCP工具提供者...");
        
        if (!openApiConfigEnabled) {
            log.info("OpenAPI配置已禁用");
            return createEmptyToolCallbackProvider();
        }
        
        if (openApiConfigFile == null || openApiConfigFile.trim().isEmpty()) {
            log.info("未指定OpenAPI配置文件，使用空的工具提供者");
            return createEmptyToolCallbackProvider();
        }
        
        try {
            log.info("正在加载OpenAPI配置文件: {}", openApiConfigFile);
            
            // 加载OpenAPI配置
            Optional<OpenApiConfig> configOptional = loadOpenApiConfig(openApiConfigFile);
            
            if (configOptional.isEmpty()) {
                log.warn("无法加载OpenAPI配置文件: {}", openApiConfigFile);
                return createEmptyToolCallbackProvider();
            }
            
            OpenApiConfig openApiConfig = configOptional.get();
            log.info("成功加载OpenAPI配置: {} v{}", 
                Optional.ofNullable(openApiConfig.getInfo())
                    .map(info -> info.getTitle())
                    .orElse("Unknown API"),
                Optional.ofNullable(openApiConfig.getInfo())
                    .map(info -> info.getVersion())
                    .orElse("Unknown")
            );
            
            // 生成动态工具
            Object dynamicTools = dynamicMcpToolGenerator.generateDynamicTools(openApiConfig);
            
            // 获取工具描述（用于日志）
            List<DynamicMcpToolGenerator.ToolDescription> toolDescriptions = 
                dynamicMcpToolGenerator.getGeneratedToolDescriptions(openApiConfig);
            
            log.info("成功生成 {} 个动态MCP工具:", toolDescriptions.size());
            toolDescriptions.forEach(tool -> 
                log.info("- {} ({}): {}", tool.getMethodName(), tool.getEndpoint(), tool.getDescription())
            );
            
            return MethodToolCallbackProvider.builder()
                .toolObjects(dynamicTools)
                .build();
                
        } catch (Exception e) {
            log.error("初始化OpenAPI MCP工具提供者失败", e);
            return createEmptyToolCallbackProvider();
        }
    }
    
    /**
     * 加载OpenAPI配置
     */
    private Optional<OpenApiConfig> loadOpenApiConfig(String configFile) {
        try {
            Resource resource = resourceLoader.getResource(configFile);
            
            if (!resource.exists()) {
                log.warn("OpenAPI配置文件不存在: {}", configFile);
                return Optional.empty();
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            boolean isYaml = configFile.toLowerCase().endsWith(".yaml") || 
                           configFile.toLowerCase().endsWith(".yml");
            
            return openApiParser.parseFromContent(content, isYaml);
            
        } catch (Exception e) {
            log.error("加载OpenAPI配置文件失败: {}", configFile, e);
            return Optional.empty();
        }
    }
    
    /**
     * 创建空的工具回调提供者
     */
    private ToolCallbackProvider createEmptyToolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
            .toolObjects(new DynamicMcpToolGenerator.EmptyToolsProxy())
            .build();
    }
    
    /**
     * 提供OpenAPI配置信息的Bean（可选，用于调试）
     */
    @Bean
    public OpenApiConfigInfo openApiConfigInfo() {
        return new OpenApiConfigInfo(
            openApiConfigFile,
            openApiConfigEnabled,
            loadOpenApiConfig(openApiConfigFile).isPresent()
        );
    }
    
    /**
     * OpenAPI配置信息
     */
    public static class OpenApiConfigInfo {
        private final String configFile;
        private final boolean enabled;
        private final boolean loaded;
        
        public OpenApiConfigInfo(String configFile, boolean enabled, boolean loaded) {
            this.configFile = configFile;
            this.enabled = enabled;
            this.loaded = loaded;
        }
        
        // Getters
        public String getConfigFile() { return configFile; }
        public boolean isEnabled() { return enabled; }
        public boolean isLoaded() { return loaded; }
        
        @Override
        public String toString() {
            return String.format("OpenApiConfigInfo{file='%s', enabled=%s, loaded=%s}",
                configFile, enabled, loaded);
        }
    }
}