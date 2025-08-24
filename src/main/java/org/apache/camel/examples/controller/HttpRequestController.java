package org.apache.camel.examples.controller;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.HttpRequestBean;
import org.apache.camel.examples.model.HttpResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/http")
public class HttpRequestController {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    @PostMapping("/request")
    public ResponseEntity<HttpResponseBean> sendHttpRequest(@RequestBody HttpRequestBean requestBean) {
        HttpResponseBean response = producerTemplate.requestBody("direct:httpRequest", requestBean, HttpResponseBean.class);
        
        // 根据HTTP响应状态码设置Spring响应状态
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
