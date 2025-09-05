package org.apache.camel.examples.domain;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.HashMap;

@Data
@AllArgsConstructor
public class HttpResponseBean {
    private int statusCode;
    private Map<String, Object> headers = new HashMap<>();
    private String body;
}