package org.apache.camel.examples.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class OpenApiToolFactory {

    private static final ParameterizedTypeReference<Map<String, Object>> TOOL_INPUT_TYPE = new ParameterizedTypeReference<>() {
    };

    private final OpenApiToolSchemaBuilder schemaBuilder;

    private final OpenApiHttpExecutor httpExecutor;

    public OpenApiToolFactory(OpenApiToolSchemaBuilder schemaBuilder, OpenApiHttpExecutor httpExecutor) {
        this.schemaBuilder = schemaBuilder;
        this.httpExecutor = httpExecutor;
    }

    public List<ToolCallback> buildTools(OpenApiDocument document) {
        if (document.openApi().getPaths() == null || document.openApi().getPaths().isEmpty()) {
            return List.of();
        }
        return document.openApi()
            .getPaths()
            .entrySet()
            .stream()
            .flatMap(entry -> createDescriptors(document, entry.getKey(), entry.getValue()).stream())
            .map(this::createCallback)
            .toList();
    }

    private ToolCallback createCallback(OpenApiToolDescriptor descriptor) {
        return FunctionToolCallback.<Map<String, Object>, Map<String, Object>>builder(descriptor.toolName(),
                (payload, context) -> this.httpExecutor.execute(descriptor, payload))
            .description(descriptor.description())
            .inputSchema(this.schemaBuilder.buildSchema(descriptor))
            .inputType(TOOL_INPUT_TYPE)
            .build();
    }

    private List<OpenApiToolDescriptor> createDescriptors(OpenApiDocument document, String path, PathItem pathItem) {
        return pathItem.readOperationsMap()
            .entrySet()
            .stream()
            .map(entry -> createDescriptor(document, path, entry.getKey(), entry.getValue(), pathItem))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<OpenApiToolDescriptor> createDescriptor(OpenApiDocument document, String path,
            PathItem.HttpMethod httpMethod, Operation operation, PathItem pathItem) {
        HttpMethod method = HttpMethod.valueOf(httpMethod.name());
        String serverUrl = resolveServerUrl(document, pathItem, operation);
        if (!StringUtils.hasText(serverUrl)) {
            return Optional.empty();
        }
        var descriptor = new OpenApiToolDescriptor(document.id(), toolName(document.id(), path, method, operation),
                toolDescription(operation, path),
                method,
                serverUrl,
                normalizePath(path),
                resolveParameters(operation, pathItem, "path"),
                resolveParameters(operation, pathItem, "query"),
                resolveParameters(operation, pathItem, "header"),
                resolveRequestBodySchema(operation),
                resolveRequestBodyMediaType(operation),
                isRequestBodyRequired(operation),
                document.defaultHeaders());
        return Optional.of(descriptor);
    }

    private String toolName(String specId, String path, HttpMethod method, Operation operation) {
        if (StringUtils.hasText(operation.getOperationId())) {
            return sanitize("%s_%s".formatted(specId, operation.getOperationId()));
        }
        String rawPath = path.replace("/", "_")
            .replace("{", "")
            .replace("}", "")
            .replaceAll("[^a-zA-Z0-9_]", "_");
        return sanitize("%s_%s%s".formatted(specId, method.name().toLowerCase(Locale.ENGLISH), rawPath));
    }

    private String toolDescription(Operation operation, String path) {
        if (StringUtils.hasText(operation.getSummary())) {
            return operation.getSummary();
        }
        if (StringUtils.hasText(operation.getDescription())) {
            return operation.getDescription();
        }
        return "Invoke HTTP endpoint %s".formatted(path);
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private List<OpenApiToolDescriptor.OpenApiParameter> resolveParameters(Operation operation, PathItem pathItem,
            String location) {
        List<Parameter> parameters = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            parameters.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            parameters.addAll(operation.getParameters());
        }
        return parameters.stream()
            .filter(parameter -> Objects.equals(parameter.getIn(), location))
            .map(parameter -> new OpenApiToolDescriptor.OpenApiParameter(parameter.getName(),
                    parameterSchema(parameter),
                    Boolean.TRUE.equals(parameter.getRequired()),
                    parameter.getDescription()))
            .toList();
    }

    private Schema<?> parameterSchema(Parameter parameter) {
        if (parameter.getSchema() != null) {
            return parameter.getSchema();
        }
        Content content = parameter.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }
        return content.values().stream().map(MediaType::getSchema).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Schema<?> resolveRequestBodySchema(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            return null;
        }
        return requestBody.getContent()
            .values()
            .stream()
            .map(MediaType::getSchema)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String resolveRequestBodyMediaType(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            return null;
        }
        return requestBody.getContent().keySet().iterator().next();
    }

    private boolean isRequestBodyRequired(Operation operation) {
        return operation.getRequestBody() != null && Boolean.TRUE.equals(operation.getRequestBody().getRequired());
    }

    private String resolveServerUrl(OpenApiDocument document, PathItem pathItem, Operation operation) {
        if (StringUtils.hasText(document.serverUrlOverride())) {
            return document.serverUrlOverride();
        }
        return StreamBuilder.from(operation.getServers())
            .or(pathItem.getServers())
            .or(document.openApi().getServers())
            .map(this::expandServerUrl)
            .orElse(null);
    }

    private String expandServerUrl(List<Server> servers) {
        return servers.stream()
            .map(this::expandServer)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private String expandServer(Server server) {
        if (server.getVariables() == null || server.getVariables().isEmpty()) {
            return server.getUrl();
        }
        String resolved = server.getUrl();
        for (Map.Entry<String, ServerVariable> entry : server.getVariables().entrySet()) {
            resolved = resolved.replace("{%s}".formatted(entry.getKey()), entry.getValue().getDefault());
        }
        return resolved;
    }

    private static class StreamBuilder {

        private Optional<List<Server>> candidate = Optional.empty();

        private StreamBuilder(List<Server> initial) {
            if (initial != null && !initial.isEmpty()) {
                this.candidate = Optional.of(initial);
            }
        }

        static StreamBuilder from(List<Server> servers) {
            return new StreamBuilder(servers);
        }

        StreamBuilder or(List<Server> servers) {
            if (this.candidate.isPresent()) {
                return this;
            }
            if (servers != null && !servers.isEmpty()) {
                this.candidate = Optional.of(servers);
            }
            return this;
        }

        Optional<String> map(java.util.function.Function<List<Server>, String> mapper) {
            Assert.notNull(mapper, "Mapper must not be null");
            return this.candidate.map(mapper);
        }
    }
}
