package org.apache.camel.examples.route;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.HttpRequestBean;
import org.apache.camel.examples.model.HttpResponseBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HttpRequestRouteIntegrationTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    void shouldProcessHttpRequestWithHeaders() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("POST");
        request.setUrl("http://httpbin.org/post");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Header", "test-value");
        headers.put("Authorization", "Bearer test-token");
        request.setHeaders(headers);
        
        request.setBody("{\"message\":\"test\"}");

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("test-value");
    }

    @Test
    void shouldHandleGetRequestWithoutBody() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/get");
        request.setHeaders(new HashMap<>());

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("get");
    }

    @Test
    void shouldTransparentlyPassErrorStatusCodeToUpstream() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/status/400");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "text/plain");
        request.setHeaders(headers);

        try {
            HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
            
            // 如果没有抛出异常，验证错误状态码被正确封装
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(400);
        } catch (Exception e) {
            // 验证异常是HTTP相关的错误
            assertThat(e.getCause()).isNotNull();
            assertThat(e.getCause().getClass().getSimpleName()).contains("Http");
        }
    }

    @Test
    void shouldTransparentlyPassResponseHeadersToUpstream() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/response-headers");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("X-Test-Header", "test-header-value");
        queryParams.put("Content-Type", "application/json");
        request.setQueryParams(queryParams);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        request.setHeaders(headers);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("X-Test-Header");
        assertThat(response.getBody()).contains("test-header-value");
        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    void shouldTransparentlyPassResponseBodyToUpstream() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("POST");
        request.setUrl("http://httpbin.org/post");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        request.setHeaders(headers);
        
        String requestBody = "{\"message\":\"response-body-test\",\"timestamp\":\"2024-01-01\"}";
        request.setBody(requestBody);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("response-body-test");
        assertThat(response.getBody()).contains("2024-01-01");
        assertThat(response.getBody()).contains("json");
        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    void shouldTransparentlyPassRequestHeadersToDownstream() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/headers");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Downstream-Header", "downstream-test-value");
        headers.put("User-Agent", "Camel-Http-Test/1.0");
        headers.put("Authorization", "Bearer downstream-token");
        request.setHeaders(headers);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        // httpbin.org/headers 会返回请求中的所有header
        assertThat(response.getBody()).contains("X-Custom-Downstream-Header");
        assertThat(response.getBody()).contains("downstream-test-value");
        assertThat(response.getBody()).contains("Camel-Http-Test/1.0");
        assertThat(response.getBody()).contains("Bearer downstream-token");
        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    void shouldTransparentlyPassRequestBodyToDownstream() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("POST");
        request.setUrl("http://httpbin.org/post");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("accept", "application/json");
        request.setHeaders(headers);
        
        String requestBody = "{\"operation\":\"request-body-test\",\"data\":{\"field1\":\"value1\",\"field2\":123}}";
        request.setBody(requestBody);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        // httpbin.org/post 会在响应中回显请求体
        assertThat(response.getBody()).contains("request-body-test");
        assertThat(response.getBody()).contains("field1");
        assertThat(response.getBody()).contains("value1");
        assertThat(response.getBody()).contains("field2");
        assertThat(response.getBody()).contains("123");
        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    void shouldHandleComplexRequestWithMultipleQueryParams() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/get");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("param1", "value1");
        queryParams.put("param2", "chinese-param");
        queryParams.put("param3", "special@characters");
        request.setQueryParams(queryParams);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        request.setHeaders(headers);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("param1");
        assertThat(response.getBody()).contains("value1");
        assertThat(response.getBody()).contains("param2");
        assertThat(response.getBody()).contains("param3");
        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    void shouldProvideCompleteHttpResponseObject() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/get");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("User-Agent", "Test-Agent");
        request.setHeaders(headers);

        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", request, HttpResponseBean.class);

        // 验证完整的响应对象结构
        assertThat(response).isNotNull();
        
        // 验证状态码和状态文本
        assertThat(response.getStatusCode()).isEqualTo(200);
        
        // 验证响应体
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
        
        // 验证响应头
        assertThat(response.getHeaders()).isNotNull();
    }
}