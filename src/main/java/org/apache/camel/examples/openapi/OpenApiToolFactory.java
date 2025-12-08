package org.apache.camel.examples.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.swagger.v3.oas.models.OpenAPI;
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

    public List<ToolCallback> buildTools(OpenApiSpecProperties.Spec spec, OpenAPI openApi) {
        Assert.notNull(openApi, "OpenAPI document must not be null");
        Assert.hasText(spec.getId(), "Spec id must not be empty");
        if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
            return List.of();
        }
        return openApi.getPaths()
            .entrySet()
            .stream()
            .flatMap(entry -> pathOperations(spec, openApi, entry.getKey(), entry.getValue()).stream())
            .toList();
    }

    private List<ToolCallback> pathOperations(OpenApiSpecProperties.Spec spec, OpenAPI openApi, String path,
            PathItem pathItem) {
        return pathItem.readOperationsMap()
            .entrySet()
            .stream()
            .map(entry -> createCallback(spec, openApi, path, pathItem, entry.getKey(), entry.getValue()))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ToolCallback> createCallback(OpenApiSpecProperties.Spec spec, OpenAPI openApi, String path,
            PathItem pathItem, PathItem.HttpMethod httpMethod, Operation operation) {
        HttpMethod method = HttpMethod.valueOf(httpMethod.name());
        String serverUrl = resolveServerUrl(spec, openApi, pathItem, operation);
        if (!StringUtils.hasText(serverUrl)) {
            return Optional.empty();
        }
        String normalizedPath = normalizePath(path);
        List<Parameter> pathParameters = resolveParameters(operation, pathItem, "path");
        List<Parameter> queryParameters = resolveParameters(operation, pathItem, "query");
        List<Parameter> headerParameters = resolveParameters(operation, pathItem, "header");
        Schema<?> requestBodySchema = resolveRequestBodySchema(operation);
        boolean requestBodyRequired = isRequestBodyRequired(operation);
        String toolName = toolName(spec.getId(), normalizedPath, method, operation);
        String description = toolDescription(operation, path);
        String inputSchema = this.schemaBuilder.buildSchema(toolName, pathParameters, queryParameters,
                headerParameters, requestBodySchema, requestBodyRequired);

        ToolCallback callback = FunctionToolCallback.<Map<String, Object>, Map<String, Object>>builder(toolName,
                (payload, context) -> this.httpExecutor.execute(method, serverUrl, normalizedPath, pathParameters,
                        queryParameters, headerParameters, requestBodySchema, requestBodyRequired,
                        spec.getDefaultHeaders(), payload))
            .description(description)
            .inputSchema(inputSchema)
            .inputType(TOOL_INPUT_TYPE)
            .build();
        return Optional.of(callback);
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

    private List<Parameter> resolveParameters(Operation operation, PathItem pathItem, String location) {
        List<Parameter> parameters = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            parameters.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            parameters.addAll(operation.getParameters());
        }
        return parameters.stream()
            .filter(parameter -> Objects.equals(parameter.getIn(), location))
            .map(this::ensureSchema)
            .toList();
    }

    private Parameter ensureSchema(Parameter parameter) {
        if (parameter.getSchema() != null) {
            return parameter;
        }
        Schema<?> schema = extractSchema(parameter);
        if (schema != null) {
            parameter.setSchema(schema);
        }
        return parameter;
    }

    private Schema<?> extractSchema(Parameter parameter) {
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

    private boolean isRequestBodyRequired(Operation operation) {
        return operation.getRequestBody() != null && Boolean.TRUE.equals(operation.getRequestBody().getRequired());
    }

    private String resolveServerUrl(OpenApiSpecProperties.Spec spec, OpenAPI openApi, PathItem pathItem,
            Operation operation) {
        if (StringUtils.hasText(spec.getServerUrlOverride())) {
            return spec.getServerUrlOverride();
        }
        return StreamBuilder.from(operation.getServers())
            .or(pathItem.getServers())
            .or(openApi.getServers())
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
