package org.apache.camel.examples.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "mcp.openapi")
public class OpenApiSpecProperties {

    private List<Spec> specs = new ArrayList<>();

    public List<Spec> getSpecs() {
        return this.specs;
    }

    public void setSpecs(List<Spec> specs) {
        this.specs = specs != null ? specs : new ArrayList<>();
    }

    public boolean hasSpecs() {
        return !this.specs.isEmpty();
    }

    public static class Spec {

        private String id;

        private String location;

        private String serverUrlOverride;

        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLocation() {
            return this.location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getServerUrlOverride() {
            return this.serverUrlOverride;
        }

        public void setServerUrlOverride(String serverUrlOverride) {
            this.serverUrlOverride = StringUtils.hasText(serverUrlOverride) ? serverUrlOverride : null;
        }

        public Map<String, String> getDefaultHeaders() {
            return this.defaultHeaders;
        }

        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders != null ? defaultHeaders : new LinkedHashMap<>();
        }
    }
}
