package org.apache.camel.examples.model;

import lombok.Data;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Data
public class HttpRequestBean {
    private String method = "GET";
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private Map<String, String> queryParams = new HashMap<>();
    
    public String buildFullUrl() {
        // 不是http param, 是camel内置参数, false时非200不再抛出异常
        queryParams.put("throwExceptionOnFailure", "false");

        return queryParams.entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&", url + "?", ""));
    }
}
