package org.apache.camel.examples.service;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * OpenAPI文档解析服务
 * 提供从不同来源读取和解析OpenAPI规范的能力
 */
@Service
public class OpenApiParserService {
    
    /**
     * 从URL加载OpenAPI文档
     * 
     * @param url OpenAPI文档的URL地址
     * @return 解析后的OpenAPI对象
     * @throws IllegalArgumentException 如果URL为空或无效
     */
    public OpenAPI parseFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL不能为空");
        }
        
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        
        var result = new OpenAPIParser().readLocation(url.trim(), null, parseOptions);
        if (result.getOpenAPI() == null) {
            throw new RuntimeException("无法从URL解析OpenAPI文档: " + url);
        }
        
        return result.getOpenAPI();
    }
    
    /**
     * 从文件路径加载OpenAPI文档
     * 
     * @param filePath OpenAPI文档的文件路径
     * @return 解析后的OpenAPI对象
     * @throws IOException 如果文件读取失败
     * @throws IllegalArgumentException 如果文件路径为空
     */
    public OpenAPI parseFromFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        String content = Files.readString(Paths.get(filePath.trim()));
        return parseFromString(content);
    }
    
    /**
     * 从字符串内容解析OpenAPI文档
     * 
     * @param content OpenAPI文档的内容（JSON或YAML格式）
     * @return 解析后的OpenAPI对象
     * @throws IllegalArgumentException 如果内容为空
     * @throws RuntimeException 如果解析失败
     */
    public OpenAPI parseFromString(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
        
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        
        var result = new OpenAPIParser().readContents(content.trim(), null, parseOptions);
        if (result.getOpenAPI() == null) {
            throw new RuntimeException("无法解析OpenAPI文档内容");
        }
        
        return result.getOpenAPI();
    }
    
    /**
     * 验证OpenAPI文档是否有效
     * 
     * @param openAPI 要验证的OpenAPI对象
     * @return true表示有效，false表示无效
     */
    public boolean isValidOpenAPI(OpenAPI openAPI) {
        if (openAPI == null) {
            return false;
        }
        
        // 基本验证：必须有info和paths
        return openAPI.getInfo() != null && 
               openAPI.getInfo().getTitle() != null && 
               !openAPI.getInfo().getTitle().trim().isEmpty();
    }
    
    /**
     * 获取OpenAPI文档的基本信息摘要
     * 
     * @param openAPI OpenAPI对象
     * @return 包含标题、版本、描述等信息的字符串
     */
    public String getOpenAPIInfo(OpenAPI openAPI) {
        if (openAPI == null || openAPI.getInfo() == null) {
            return "无效的OpenAPI文档";
        }
        
        var info = openAPI.getInfo();
        StringBuilder summary = new StringBuilder();
        summary.append("标题: ").append(info.getTitle()).append("\n");
        summary.append("版本: ").append(info.getVersion()).append("\n");
        
        if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
            summary.append("描述: ").append(info.getDescription()).append("\n");
        }
        
        if (openAPI.getPaths() != null) {
            summary.append("API端点数量: ").append(openAPI.getPaths().size()).append("\n");
        }
        
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            String serverUrl = openAPI.getServers().get(0).getUrl();
            // 只有在服务器URL不是默认的"/"时才显示
            if (serverUrl != null && !serverUrl.equals("/")) {
                summary.append("服务器: ").append(serverUrl).append("\n");
            }
        }
        
        return summary.toString();
    }
}