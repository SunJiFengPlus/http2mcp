package org.apache.camel.examples.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.apache.camel.examples.model.HttpRequestBean;
import org.apache.camel.examples.model.HttpResponseBean;

import java.util.Map;
import java.util.HashMap;

@Component
public class HttpRequestRoute extends RouteBuilder {
    
    @Override
    public void configure() {
        from("direct:httpRequest")
            .routeId("dynamicHttpRequest")
            .log("收到HTTP请求: ${body}")
            .process(this::processHttpRequest)
            .recipientList(header("HTTP_ENDPOINT"))
            .log("HTTP请求完成，响应: ${body}")
            .process(this::processHttpResponse);
    }
    
    private void processHttpRequest(Exchange exchange) {
        HttpRequestBean request = exchange.getIn().getBody(HttpRequestBean.class);
        
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, request.getMethod());
        exchange.getIn().setHeader("HTTP_ENDPOINT", request.buildFullUrl());
        request.getHeaders().forEach((k, v) -> exchange.getIn().setHeader(k, v));
        
        String body = request.getBody();
        exchange.getIn().setBody(body != null && !body.trim().isEmpty() ? body : "");
    }
    
    private void processHttpResponse(Exchange exchange) {
        // 获取响应信息
        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String responseBody = exchange.getIn().getBody(String.class);
        
        // 创建响应对象
        HttpResponseBean response = new HttpResponseBean();
        response.setStatusCode(statusCode != null ? statusCode : 200);
        response.setStatusText(getStatusText(response.getStatusCode()));
        response.setBody(responseBody);
        
        // 获取响应头
        Map<String, Object> responseHeaders = new HashMap<>();
        Map<String, Object> exchangeHeaders = exchange.getIn().getHeaders();
        
        // 过滤并收集HTTP响应头
        exchangeHeaders.entrySet().stream()
            .filter(entry -> isHttpResponseHeader(entry.getKey()))
            .forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
        
        response.setHeaders(responseHeaders);
        
        // 设置Content-Type
        Object contentType = exchange.getIn().getHeader("Content-Type");
        if (contentType != null) {
            response.setContentType(contentType.toString());
        }
        
        // 将封装的响应对象设置为消息体
        exchange.getIn().setBody(response);
    }
    
    private boolean isHttpResponseHeader(String headerName) {
        return headerName != null && (
            headerName.startsWith("Content-") ||
            headerName.startsWith("Cache-") ||
            headerName.startsWith("X-") ||
            headerName.equals("Date") ||
            headerName.equals("Server") ||
            headerName.equals("Location") ||
            headerName.equals("Set-Cookie") ||
            headerName.equals("Transfer-Encoding") ||
            headerName.equals("Connection") ||
            headerName.equals("Vary") ||
            headerName.equals("ETag") ||
            headerName.equals("Last-Modified")
        );
    }
    
    private String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown";
        };
    }
}
