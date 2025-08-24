package org.apache.camel.examples.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponseBean {
    private int statusCode;
    private String statusText;
    private Map<String, Object> headers = new HashMap<>();
    private String body;
    private String contentType;
    
    public static HttpResponseBean success(String body) {
        HttpResponseBean response = new HttpResponseBean();
        response.setStatusCode(200);
        response.setStatusText("OK");
        response.setBody(body);
        return response;
    }
    
    public static HttpResponseBean error(int statusCode, String statusText, String body) {
        HttpResponseBean response = new HttpResponseBean();
        response.setStatusCode(statusCode);
        response.setStatusText(statusText);
        response.setBody(body);
        return response;
    }
}