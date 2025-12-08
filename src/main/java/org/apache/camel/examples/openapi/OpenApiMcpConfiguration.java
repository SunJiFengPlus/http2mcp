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
        List<ToolCallback> callbacks = properties.getSpecs()
            .stream()
            .flatMap(spec -> factory.buildTools(spec, loader.load(spec)).stream())
            .toList();
        return ToolCallbackProvider.from(callbacks);
    }
}
