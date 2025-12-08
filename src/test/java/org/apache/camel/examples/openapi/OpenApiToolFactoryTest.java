package org.apache.camel.examples.openapi;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiToolFactoryTest {

    private final OpenApiDocumentLoader loader = new OpenApiDocumentLoader(new DefaultResourceLoader());

    private final OpenApiToolSchemaBuilder schemaBuilder = new OpenApiToolSchemaBuilder(new ObjectMapper());

    private final OpenApiHttpExecutor httpExecutor = new OpenApiHttpExecutor(WebClient.builder(),
            new ObjectMapper());

    private final OpenApiToolFactory factory = new OpenApiToolFactory(schemaBuilder, httpExecutor);

    @Test
    void shouldBuildCallbacksPerOperation() {
        OpenApiSpecProperties properties = new OpenApiSpecProperties();
        properties.setSpecs(List.of(spec("json", "classpath:/openapi/httpbin-post.json")));
        OpenApiDocument document = loader.load(properties).get(0);

        List<ToolCallback> callbacks = factory.buildTools(document);

        assertThat(callbacks).hasSize(1);
        assertThat(callbacks.get(0).getToolDefinition().name()).isEqualTo("json_postAnything");
        assertThat(callbacks.get(0).getToolDefinition().inputSchema()).contains("\"body\"");
    }

    private OpenApiSpecProperties.Spec spec(String id, String location) {
        OpenApiSpecProperties.Spec spec = new OpenApiSpecProperties.Spec();
        spec.setId(id);
        spec.setLocation(location);
        return spec;
    }
}
