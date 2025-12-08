package org.apache.camel.examples.openapi;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenApiSpecProperties.class)
public class OpenApiMcpConfiguration {

    @Bean
    public ToolCallbackProvider openApiToolCallbackProvider(OpenApiSpecProperties properties,
            OpenApiDocumentLoader loader, OpenApiToolFactory factory) {
        List<ToolCallback> callbacks = loader.load(properties)
            .stream()
            .flatMap(document -> factory.buildTools(document).stream())
            .toList();
        return ToolCallbackProvider.from(callbacks);
    }
}
