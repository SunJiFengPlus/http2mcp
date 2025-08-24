package org.apache.camel.examples.route;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.HttpRequestBean;
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
    private CamelContext camelContext;

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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        assertThat(response).contains("test-value");
    }

    @Test
    void shouldHandleGetRequestWithoutBody() {
        HttpRequestBean request = new HttpRequestBean();
        request.setMethod("GET");
        request.setUrl("http://httpbin.org/get");
        request.setHeaders(new HashMap<>());

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        assertThat(response).contains("get");
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
            String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
            
            // httpbin.org/status/400 会返回400状态码
            // Camel会抛出HttpOperationFailedException，这是预期行为
            assertThat(response).isNotNull();
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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        assertThat(response).contains("X-Test-Header");
        assertThat(response).contains("test-header-value");
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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        assertThat(response).contains("response-body-test");
        assertThat(response).contains("2024-01-01");
        assertThat(response).contains("json");
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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        // httpbin.org/headers 会返回请求中的所有header
        assertThat(response).contains("X-Custom-Downstream-Header");
        assertThat(response).contains("downstream-test-value");
        assertThat(response).contains("Camel-Http-Test/1.0");
        assertThat(response).contains("Bearer downstream-token");
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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        // httpbin.org/post 会在响应中回显请求体
        assertThat(response).contains("request-body-test");
        assertThat(response).contains("field1");
        assertThat(response).contains("value1");
        assertThat(response).contains("field2");
        assertThat(response).contains("123");
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

        String response = producerTemplate.requestBody("direct:httpRequest", request, String.class);
        
        assertThat(response).isNotNull();
        assertThat(response).contains("param1");
        assertThat(response).contains("value1");
        assertThat(response).contains("param2");
        assertThat(response).contains("param3");
    }
}