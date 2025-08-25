package org.apache.camel.examples.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.apache.camel.examples.model.HttpRequestBean;
import org.apache.camel.examples.model.HttpResponseBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HttpRequestRoute extends RouteBuilder {
    
    public static final Set<String> CAMEL_HEADER_BUT_NOT_HTTP = Set.of(
        "CamelHttpMethod",
        "CamelHttpResponseCode",
        "CamelHttpResponseText",
        "HTTP_ENDPOINT",
        "accept"
    );
    
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
        Optional.ofNullable(request.getHeaders())
            .orElse(new HashMap<>())
            .forEach((k, v) -> exchange.getIn().setHeader(k, v));

        String body = request.getBody();
        exchange.getIn().setBody(body != null && !body.trim().isEmpty() ? body : "");
    }
    
    private void processHttpResponse(Exchange exchange) {
        // 获取响应信息
        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String responseBody = exchange.getIn().getBody(String.class);
        
        // 获取响应头
        Map<String, Object> responseHeaders = exchange.getIn().getHeaders()
            .entrySet().stream()
            .filter(entry -> isHttpResponseHeader(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 创建响应对象
        HttpResponseBean response = new HttpResponseBean(statusCode, responseHeaders, responseBody);

        exchange.getIn().setBody(response);
    }
    
    private boolean isHttpResponseHeader(String headerName) {
        return !CAMEL_HEADER_BUT_NOT_HTTP.contains(headerName);
    }
}
