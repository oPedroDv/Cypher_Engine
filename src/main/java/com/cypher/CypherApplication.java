package com.cypher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cypher")
public class CypherApplication {
    public static void main(String[] args) {
        SpringApplication.run(CypherApplication.class, args);
    }
}