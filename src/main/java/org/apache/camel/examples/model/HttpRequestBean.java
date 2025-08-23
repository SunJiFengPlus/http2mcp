package org.apache.camel.examples.model;

import lombok.Data;

import java.util.Map;
import java.util.HashMap;

@Data
public class HttpRequestBean {
    private String method = "GET";
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private Map<String, String> queryParams = new HashMap<>();
    
    public String buildFullUrl() {
        if (queryParams.isEmpty()) {
            return url;
        }
        
        StringBuilder fullUrl = new StringBuilder(url);
        fullUrl.append("?");
        
        queryParams.forEach((key, value) -> fullUrl.append(key)
            .append("=")
            .append(value)
            .append("&"));
        
        return fullUrl.substring(0, fullUrl.length() - 1);
    }
}
