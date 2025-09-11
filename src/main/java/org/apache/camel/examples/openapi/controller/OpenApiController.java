package org.apache.camel.examples.openapi.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.apache.camel.examples.openapi.service.OpenApiYamlReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAPI控制器
 * 提供读取和查看OpenAPI规范的REST接口
 */
@RestController
@RequestMapping("/api/openapi")
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiYamlReader openApiYamlReader;
    
    @Value("${openapi.file:classpath:openapi/example-api.yaml}")
    private String defaultOpenApiFile;

    /**
     * 读取并返回OpenAPI规范的基本信息
     */
    @GetMapping("/info")
    public Map<String, Object> getOpenApiInfo(
            @RequestParam(required = false) String file) {
        
        String filePath = file != null ? file : defaultOpenApiFile;
        
        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource(filePath);
        
        if (openAPI.isPresent()) {
            OpenAPI api = openAPI.get();
            result.put("success", true);
            
            if (api.getInfo() != null) {
                Info info = api.getInfo();
                result.put("title", info.getTitle());
                result.put("version", info.getVersion());
                result.put("description", info.getDescription());
            }
            
            if (api.getPaths() != null) {
                result.put("pathCount", api.getPaths().size());
                result.put("paths", api.getPaths().keySet());
            }
            
            if (api.getServers() != null && !api.getServers().isEmpty()) {
                result.put("servers", api.getServers().stream()
                    .map(server -> Map.of("url", server.getUrl(), "description", server.getDescription()))
                    .toList());
            }
            
        } else {
            result.put("success", false);
            result.put("error", "无法读取OpenAPI文件");
        }
        
        return result;
    }

    /**
     * 返回完整的OpenAPI规范
     */
    @GetMapping("/spec")
    public Object getOpenApiSpec(@RequestParam(required = false) String file) {
        String filePath = file != null ? file : defaultOpenApiFile;
        
        Optional<OpenAPI> openAPI = openApiYamlReader.readOpenApiFromResource(filePath);
        
        if (openAPI.isPresent()) {
            return openAPI.get();
        } else {
            return Map.of(
                "error", "无法读取OpenAPI文件",
                "filePath", filePath
            );
        }
    }
}