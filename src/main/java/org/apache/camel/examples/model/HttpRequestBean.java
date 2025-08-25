package org.apache.camel.examples.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap;

@Data
@AllArgsConstructor
public class HttpRequestBean {
    private String method;
    private String url;
    private Map<String, String> headers;
    private String body;
    private Map<String, String> queryParams;
    
    public String buildFullUrl() {
        if (Objects.isNull(queryParams)) {
            queryParams = new HashMap<>();
        }
        
        // 不是http param, 是camel内置参数, false时非200不再抛出异常
        queryParams.put("throwExceptionOnFailure", "false");

        return queryParams.entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&", url + "?", ""));
    }
}
