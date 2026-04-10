package ru.practicum.interaction_api.contract.event_service;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventClientConfig {
    @Bean
    public ErrorDecoder eventErrorDecoder() {
        return new EventClientErrorDecoder();
    }
}
