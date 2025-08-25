package org.apache.camel.examples.web;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.HttpRequestBean;
import org.apache.camel.examples.model.HttpResponseBean;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HttpRequestController {
    
    @Autowired
    ProducerTemplate producerTemplate;

    @PostMapping("/http/request")
    public ResponseEntity<HttpResponseBean> sendHttpRequest(@RequestBody HttpRequestBean requestBean) {
        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
        
        // 根据HTTP响应状态码设置Spring响应状态
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
    
    @Tool(description = "发起HTTP请求")
    public HttpResponseBean httpRequest(@ToolParam(description = "HTTP请求方法") String method,
                                        @ToolParam(description = "HTTP请求URL, 例如: http://httpbin.org/get") String url,
                                        @ToolParam(description = "HTTP请求头", required = false) Map<String, String> headers,
                                        @ToolParam(description = "HTTP请求体", required = false) String body,
                                        @ToolParam(description = "HTTP请求查询参数", required = false) Map<String, String> queryParams
    ) {
        HttpRequestBean requestBean = new HttpRequestBean(method, url, headers, body, queryParams);
        return producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
    }
}
