package org.apache.camel.examples.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * OpenAPI YAML文件读取服务
 * 从resources中读取OpenAPI规范文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiYamlReader {
    
    private final ResourceLoader resourceLoader;
    
    /**
     * 从资源文件中读取OpenAPI规范
     * 
     * @param resourcePath 资源路径，例如 "classpath:openapi/api.yaml"
     * @return OpenAPI对象，如果读取失败则返回空
     */
    public Optional<OpenAPI> readOpenApiFromResource(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            log.warn("OpenAPI资源路径为空");
            return Optional.empty();
        }
        
        try {
            Resource resource = resourceLoader.getResource(resourcePath);
            
            if (!resource.exists()) {
                log.warn("OpenAPI资源文件不存在: {}", resourcePath);
                return Optional.empty();
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(content, null, null).getOpenAPI();
            
            if (openAPI == null) {
                log.warn("无法解析OpenAPI内容");
                return Optional.empty();
            }
            
            log.info("成功读取OpenAPI规范: {} v{}", 
                openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown API",
                openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "Unknown"
            );
            
            return Optional.of(openAPI);
            
        } catch (Exception e) {
            log.error("读取OpenAPI资源文件失败: {}", resourcePath, e);
            return Optional.empty();
        }
    }
}