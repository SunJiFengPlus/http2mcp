package org.apache.camel.examples.openapi;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18888",
                "spring.main.web-application-type=reactive",
                "spring.ai.mcp.server.base-url=http://localhost:18888",
                "spring.ai.mcp.server.sse-endpoint=/it/sse",
                "spring.ai.mcp.server.sse-message-endpoint=/it/message",
                "spring.ai.mcp.server.stdio=false",
                "spring.ai.mcp.server.capabilities.resource=false",
                "spring.ai.mcp.server.capabilities.prompt=false",
                "spring.ai.mcp.server.capabilities.completion=false",
                "mcp.openapi.specs[0].id=query",
                "mcp.openapi.specs[0].location=classpath:/openapi/httpbin-query.yaml",
                "mcp.openapi.specs[1].id=post",
                "mcp.openapi.specs[1].location=classpath:/openapi/httpbin-post.json",
                "mcp.openapi.specs[2].id=status",
                "mcp.openapi.specs[2].location=classpath:/openapi/httpbin-status.yaml"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HttpBinMcpIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldExposeHttpbinOperationsAsMcpTools() {
        try (McpSyncClient client = createClient()) {
            client.initialize();

            McpSchema.ListToolsResult toolsResult = client.listTools();

            assertThat(toolNames(toolsResult)).contains("query_queryEcho", "post_postAnything", "status_statusCheck");
        }
    }

    @Test
    void shouldInvokeGeneratedToolsViaMcpProtocol() {
        try (McpSyncClient client = createClient()) {
            client.initialize();

            McpSchema.CallToolResult queryResult = client
                .callTool(new McpSchema.CallToolRequest("query_queryEcho", Map.of("foo", "hello")));
            McpSchema.CallToolResult postResult = client
                .callTool(new McpSchema.CallToolRequest("post_postAnything",
                        Map.of("body", Map.of("message", "from-mcp"))));

            assertThat(readBody(queryResult)).extracting("status").isEqualTo(200);
            assertThat(nested(readBody(queryResult), "body", "args")).extracting("foo").isEqualTo("hello");
            assertThat(nested(readBody(postResult), "body", "json")).extracting("message").isEqualTo("from-mcp");
        }
    }

    private McpSyncClient createClient() {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://localhost:18888")
            .sseEndpoint("/it/sse")
            .build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(20)).build();
    }

    private List<String> toolNames(McpSchema.ListToolsResult result) {
        return result.tools().stream().map(McpSchema.Tool::name).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readBody(McpSchema.CallToolResult result) {
        try {
            McpSchema.TextContent content = (McpSchema.TextContent) result.content().get(0);
            return OBJECT_MAPPER.readValue(content.text(), new TypeReference<Map<String, Object>>() {
            });
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to parse MCP response", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nested(Map<String, Object> source, String... keys) {
        Map<String, Object> current = source;
        for (String key : keys) {
            Object value = current.get(key);
            assertThat(value).isInstanceOf(Map.class);
            current = (Map<String, Object>) value;
        }
        return current;
    }
}
