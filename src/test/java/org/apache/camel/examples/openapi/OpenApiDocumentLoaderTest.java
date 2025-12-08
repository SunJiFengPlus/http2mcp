package org.apache.camel.examples.openapi;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentLoaderTest {

    private final OpenApiDocumentLoader loader = new OpenApiDocumentLoader(new DefaultResourceLoader());

    @Test
    void shouldLoadYamlAndJsonDocuments() {
        OpenApiSpecProperties properties = new OpenApiSpecProperties();
        properties.setSpecs(List.of(spec("yaml", "classpath:/openapi/httpbin-query.yaml"),
                spec("json", "classpath:/openapi/httpbin-post.json")));

        List<OpenApiDocument> documents = loader.load(properties);

        assertThat(documents).hasSize(2);
        assertThat(documents)
            .extracting(OpenApiDocument::id)
            .containsExactlyInAnyOrder("yaml", "json");
        assertThat(documents.get(0).openApi().getPaths()).isNotEmpty();
    }

    private OpenApiSpecProperties.Spec spec(String id, String location) {
        OpenApiSpecProperties.Spec spec = new OpenApiSpecProperties.Spec();
        spec.setId(id);
        spec.setLocation(location);
        return spec;
    }
}
