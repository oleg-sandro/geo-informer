package com.example.geoinformer.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.example.geoinformer.controller", "com.example.geoinformer.service"})
@EnableJpaRepositories(basePackages = "com.example.geoinformer.repository")
@EnableTransactionManagement
@EntityScan(basePackages = "com.example.geoinformer.entity")
public class Application {

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }
}