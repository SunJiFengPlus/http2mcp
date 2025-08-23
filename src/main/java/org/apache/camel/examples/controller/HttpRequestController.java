package org.apache.camel.examples.controller;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.examples.model.HttpRequestBean;
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
    public ResponseEntity<String> sendHttpRequest(@RequestBody HttpRequestBean requestBean) {
        String response = producerTemplate.requestBody("direct:httpRequest", requestBean, String.class);
        return ResponseEntity.ok(response);
    }
}
