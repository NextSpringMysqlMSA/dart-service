/**
 * @file WebClientConfig.java
 * @description WebClient 빈을 구성하는 클래스입니다.
 */
package com.example.javaversion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
} 