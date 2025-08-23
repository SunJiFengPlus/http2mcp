package org.apache.camel.examples.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.apache.camel.examples.model.HttpRequestBean;

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
        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (statusCode != null) {
            exchange.getIn().setHeader("RESPONSE_STATUS_CODE", statusCode);
        }
    }
}
