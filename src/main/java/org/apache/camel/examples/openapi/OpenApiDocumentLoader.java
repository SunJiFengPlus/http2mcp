package org.apache.camel.examples.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.models.OpenAPI;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

@Component
public class OpenApiDocumentLoader {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiDocumentLoader.class);

    private final ResourceLoader resourceLoader;

    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    public OpenApiDocumentLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public OpenAPI load(OpenApiSpecProperties.Spec spec) {
        Assert.hasText(spec.getId(), "Spec id must not be empty");
        Assert.hasText(spec.getLocation(), "Spec location must not be empty");
        Resource resource = this.resourceLoader.getResource(spec.getLocation());
        Assert.state(resource.exists(), () -> "OpenAPI resource not found: " + spec.getLocation());
        String document = readDocument(resource);
        SwaggerParseResult result = this.parser.readContents(document, null, createParseOptions());
        logParserMessages(spec, result);
        Assert.notNull(result.getOpenAPI(),
                () -> "Unable to parse OpenAPI definition for spec %s".formatted(spec.getId()));
        return result.getOpenAPI();
    }

    private static ParseOptions createParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setFlatten(true);
        return options;
    }

    private static String readDocument(Resource resource) {
        try (var inputStream = resource.getInputStream()) {
            return new String(FileCopyUtils.copyToByteArray(inputStream), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read OpenAPI resource %s".formatted(resource.getDescription()),
                    ex);
        }
    }

    private static void logParserMessages(OpenApiSpecProperties.Spec spec, SwaggerParseResult result) {
        if (result.getMessages() == null || result.getMessages().isEmpty()) {
            return;
        }
        result.getMessages().forEach(message -> logger.warn("OpenAPI parse message for {}: {}", spec.getId(),
                message));
    }
}
