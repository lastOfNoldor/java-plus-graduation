package ru.practicum.category_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction_api.contract.event_service.EventClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventGatewayService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "event-service", fallbackMethod = "fallbackExistsByCategoryId")
    public boolean existsByCategoryId(Long catId) {
        return eventClient.existsByCategoryId(catId);
    }

    private boolean fallbackExistsByCategoryId(Long catId, Throwable t) {
        throw new RuntimeException("Event service is temporarily unavailable. Please try again later.");
    }



}
