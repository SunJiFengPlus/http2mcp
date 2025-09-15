package org.apache.camel.examples.service;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenAPI MCP工具管理器
 * 负责管理和协调OpenAPI到MCP工具的转换和生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiMcpToolsManager {

    private final OpenApiParserService openApiParserService;
    private final OpenApiToMcpToolsService openApiToMcpToolsService;
    private final DynamicMcpToolsGenerator dynamicMcpToolsGenerator;
    
    @Value("${openapi.default-spec-path:#{null}}")
    private String defaultSpecPath;
    
    @Value("${openapi.auto-load-directory:#{null}}")
    private String autoLoadDirectory;
    
    @Value("${openapi.auto-load-on-startup:true}")
    private boolean autoLoadOnStartup;

    /**
     * 应用启动后自动加载OpenAPI规范
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoLoadOnStartup) {
            log.info("OpenAPI自动加载已禁用");
            return;
        }

        try {
            if (defaultSpecPath != null && !defaultSpecPath.trim().isEmpty()) {
                log.info("加载默认OpenAPI规范: {}", defaultSpecPath);
                loadOpenApiSpec(defaultSpecPath);
            }

            if (autoLoadDirectory != null && !autoLoadDirectory.trim().isEmpty()) {
                log.info("扫描OpenAPI规范目录: {}", autoLoadDirectory);
                loadOpenApiSpecsFromDirectory(autoLoadDirectory);
            }

            if (defaultSpecPath == null && autoLoadDirectory == null) {
                log.info("未配置默认OpenAPI规范路径，跳过自动加载");
            }
        } catch (Exception e) {
            log.error("自动加载OpenAPI规范失败", e);
        }
    }

    /**
     * 手动加载OpenAPI规范文件
     */
    @Tool(description = "从文件加载OpenAPI规范并生成MCP工具")
    public String loadOpenApiSpec(@ToolParam(description = "OpenAPI规范文件路径") String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return "错误: 文件路径不能为空";
            }

            log.info("开始加载OpenAPI规范: {}", filePath);
            
            // 验证文件是否存在
            Path path = Paths.get(filePath.trim());
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + filePath;
            }

            // 解析OpenAPI规范
            OpenAPI openAPI = openApiParserService.parseFromFile(filePath);
            if (!openApiParserService.isValidOpenAPI(openAPI)) {
                return "错误: 无效的OpenAPI规范";
            }

            // 注册静态工具
            openApiToMcpToolsService.registerToolsFromOpenApi(openAPI);
            
            // 注册动态工具
            dynamicMcpToolsGenerator.loadOpenApiSpec(openAPI);

            String apiTitle = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "未知API";
            int staticToolsCount = openApiToMcpToolsService.getRegisteredTools().size();
            int dynamicToolsCount = dynamicMcpToolsGenerator.getToolDefinitionsCount();

            String result = String.format(
                "成功加载OpenAPI规范: %s\n" +
                "API标题: %s\n" +
                "静态工具数量: %d\n" +
                "动态工具数量: %d\n" +
                "总计: %d个工具",
                filePath, apiTitle, staticToolsCount, dynamicToolsCount, 
                staticToolsCount + dynamicToolsCount
            );

            log.info(result);
            return result;

        } catch (IOException e) {
            String error = "文件读取失败: " + e.getMessage();
            log.error(error, e);
            return "错误: " + error;
        } catch (Exception e) {
            String error = "加载OpenAPI规范失败: " + e.getMessage();
            log.error(error, e);
            return "错误: " + error;
        }
    }

    /**
     * 从目录批量加载OpenAPI规范
     */
    @Tool(description = "从指定目录批量加载OpenAPI规范文件")
    public String loadOpenApiSpecsFromDirectory(
            @ToolParam(description = "包含OpenAPI规范文件的目录路径") String directoryPath) {
        try {
            if (directoryPath == null || directoryPath.trim().isEmpty()) {
                return "错误: 目录路径不能为空";
            }

            Path dir = Paths.get(directoryPath.trim());
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return "错误: 目录不存在或不是有效目录 - " + directoryPath;
            }

            List<String> supportedExtensions = List.of(".yaml", ".yml", ".json");
            
            List<Path> specFiles;
            try (Stream<Path> paths = Files.walk(dir, 2)) { // 限制递归深度为2
                specFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return supportedExtensions.stream().anyMatch(fileName::endsWith);
                    })
                    .collect(Collectors.toList());
            }

            if (specFiles.isEmpty()) {
                return "在目录中未找到OpenAPI规范文件 (支持的扩展名: " + 
                       String.join(", ", supportedExtensions) + ")";
            }

            StringBuilder results = new StringBuilder();
            int successCount = 0;
            int failCount = 0;

            results.append(String.format("在目录 %s 中找到 %d 个规范文件:\n\n", directoryPath, specFiles.size()));

            for (Path specFile : specFiles) {
                try {
                    String result = loadOpenApiSpec(specFile.toString());
                    if (result.startsWith("成功")) {
                        successCount++;
                        results.append("✓ ").append(specFile.getFileName()).append(" - 成功\n");
                    } else {
                        failCount++;
                        results.append("✗ ").append(specFile.getFileName()).append(" - ").append(result).append("\n");
                    }
                } catch (Exception e) {
                    failCount++;
                    results.append("✗ ").append(specFile.getFileName()).append(" - 异常: ").append(e.getMessage()).append("\n");
                }
            }

            results.append(String.format("\n批量加载完成: %d 成功, %d 失败", successCount, failCount));
            return results.toString();

        } catch (IOException e) {
            String error = "目录访问失败: " + e.getMessage();
            log.error(error, e);
            return "错误: " + error;
        } catch (Exception e) {
            String error = "批量加载失败: " + e.getMessage();
            log.error(error, e);
            return "错误: " + error;
        }
    }

    /**
     * 获取当前已加载的工具概览
     */
    @Tool(description = "获取当前所有已加载的OpenAPI工具概览")
    public Map<String, Object> getToolsOverview() {
        Map<String, OpenApiToMcpToolsService.ToolInfo> staticTools = openApiToMcpToolsService.getRegisteredTools();
        int dynamicToolsCount = dynamicMcpToolsGenerator.getToolDefinitionsCount();
        OpenAPI currentOpenAPI = dynamicMcpToolsGenerator.getCurrentOpenAPI();

        Map<String, Object> overview = Map.of(
            "staticToolsCount", staticTools.size(),
            "dynamicToolsCount", dynamicToolsCount,
            "totalToolsCount", staticTools.size() + dynamicToolsCount,
            "hasLoadedOpenAPI", currentOpenAPI != null,
            "apiTitle", currentOpenAPI != null && currentOpenAPI.getInfo() != null ? 
                       currentOpenAPI.getInfo().getTitle() : "无",
            "staticTools", staticTools.keySet(),
            "configuredDefaultPath", defaultSpecPath != null ? defaultSpecPath : "未配置",
            "configuredAutoLoadDir", autoLoadDirectory != null ? autoLoadDirectory : "未配置",
            "autoLoadEnabled", autoLoadOnStartup
        );

        return overview;
    }

    /**
     * 重新加载所有工具
     */
    @Tool(description = "清空并重新加载所有OpenAPI工具")
    public String reloadAllTools() {
        try {
            // 清空现有工具
            openApiToMcpToolsService.clearRegisteredTools();
            dynamicMcpToolsGenerator.clearToolDefinitions();
            
            log.info("已清空所有工具，开始重新加载");

            // 重新自动加载
            onApplicationReady();

            return "重新加载完成。当前工具数量: " + 
                   (openApiToMcpToolsService.getRegisteredTools().size() + 
                    dynamicMcpToolsGenerator.getToolDefinitionsCount());

        } catch (Exception e) {
            String error = "重新加载失败: " + e.getMessage();
            log.error(error, e);
            return "错误: " + error;
        }
    }

    /**
     * 验证OpenAPI规范文件
     */
    @Tool(description = "验证OpenAPI规范文件的有效性")
    public String validateOpenApiSpec(@ToolParam(description = "OpenAPI规范文件路径") String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return "错误: 文件路径不能为空";
            }

            Path path = Paths.get(filePath.trim());
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + filePath;
            }

            OpenAPI openAPI = openApiParserService.parseFromFile(filePath);
            boolean isValid = openApiParserService.isValidOpenAPI(openAPI);

            if (!isValid) {
                return "OpenAPI规范无效: 缺少必需的info或title字段";
            }

            // 收集规范信息
            StringBuilder info = new StringBuilder();
            info.append("OpenAPI规范验证通过!\n\n");
            
            if (openAPI.getInfo() != null) {
                info.append("标题: ").append(openAPI.getInfo().getTitle()).append("\n");
                info.append("版本: ").append(openAPI.getInfo().getVersion()).append("\n");
                if (openAPI.getInfo().getDescription() != null) {
                    info.append("描述: ").append(openAPI.getInfo().getDescription()).append("\n");
                }
            }

            if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
                info.append("服务器: ").append(openAPI.getServers().get(0).getUrl()).append("\n");
            }

            if (openAPI.getPaths() != null) {
                info.append("路径数量: ").append(openAPI.getPaths().size()).append("\n");
                long operationCount = openAPI.getPaths().values().stream()
                    .mapToLong(pathItem -> pathItem.readOperationsMap().size())
                    .sum();
                info.append("操作数量: ").append(operationCount);
            }

            return info.toString();

        } catch (IOException e) {
            return "文件读取失败: " + e.getMessage();
        } catch (Exception e) {
            return "验证失败: " + e.getMessage();
        }
    }
}