package org.apache.camel.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 简单的Spring Boot应用
 * 提供从resources中读取OpenAPI YAML文件的能力
 */
@SpringBootApplication
public class CamelHttpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamelHttpDemoApplication.class, args);
    }
}
