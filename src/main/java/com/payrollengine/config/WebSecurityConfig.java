package com.payrollengine.config;

import com.payrollengine.repository.IdempotencyRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(@Value("${app.api-key:}") String apiKey) {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(new ApiKeyFilter(apiKey));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyKeyFilter> idempotencyKeyFilter(IdempotencyRecordRepository repository) {
        FilterRegistrationBean<IdempotencyKeyFilter> registration =
                new FilterRegistrationBean<>(new IdempotencyKeyFilter(repository));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2); // after the API key check
        return registration;
    }
}
