package org.apache.camel.examples.openapi;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentLoaderTest {

    private final OpenApiDocumentLoader loader = new OpenApiDocumentLoader(new DefaultResourceLoader());

    @Test
    void shouldLoadYamlAndJsonDocuments() {
        OpenApiSpecProperties.Spec yamlSpec = spec("yaml", "classpath:/openapi/httpbin-query.yaml");
        OpenApiSpecProperties.Spec jsonSpec = spec("json", "classpath:/openapi/httpbin-post.json");

        assertThat(this.loader.load(yamlSpec).getPaths()).isNotEmpty();
        assertThat(this.loader.load(jsonSpec).getPaths()).containsKey("/anything");
    }

    private OpenApiSpecProperties.Spec spec(String id, String location) {
        OpenApiSpecProperties.Spec spec = new OpenApiSpecProperties.Spec();
        spec.setId(id);
        spec.setLocation(location);
        return spec;
    }
}
