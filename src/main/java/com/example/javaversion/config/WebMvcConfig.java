/**
 * @file WebConfig.java
 * @description Web MVC 설정을 위한 Configuration 클래스입니다.
 */
package com.example.javaversion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * HTTP 메시지 컨버터 설정
     * UTF-8 인코딩을 명시적으로 설정하여 한글 인코딩 문제를 해결합니다.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(0, stringConverter);
    }
}
