package com.example.camelmcp.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LegacyController {

    @GetMapping(path = "/legacy", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> legacy(@RequestParam(value = "q", required = false) String q) {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("message", Map.of("content", "hello"));
        Map<String, Object> m2 = new HashMap<>();
        m2.put("message", Map.of("content", q == null ? "world" : q));
        return Map.of("choices", List.of(m1, m2));
    }
}

