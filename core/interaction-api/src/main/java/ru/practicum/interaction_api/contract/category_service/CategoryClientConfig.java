package ru.practicum.interaction_api.contract.category_service;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoryClientConfig {
    @Bean
    public ErrorDecoder categoryErrorDecoder() {
        return new CategoryClientErrorDecoder();
    }
}
