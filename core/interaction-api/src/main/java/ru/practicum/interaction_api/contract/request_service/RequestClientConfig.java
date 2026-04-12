package ru.practicum.interaction_api.contract.request_service;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestClientConfig {
    @Bean
    public ErrorDecoder requestErrorDecoder() {
        return new RequestClientErrorDecoder();
    }
}
