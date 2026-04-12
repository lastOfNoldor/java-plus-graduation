package ru.practicum.interaction_api.contract.user_service;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserClientConfig {
    @Bean
    public ErrorDecoder userErrorDecoder() {
        return new UserClientErrorDecoder();
    }
}
