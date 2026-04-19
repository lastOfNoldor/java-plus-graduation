package ru.practicum.event_service.event.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction_api.contract.request_service.RequestClient;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RequestGatewayService {
    private final RequestClient requestClient;


    @CircuitBreaker(name = "request-service", fallbackMethod = "fallbackHasConfirmedRequest")
    public boolean hasConfirmedRequest(Long userId, Long eventId) {
        return requestClient.hasConfirmedRequest(userId, eventId);
    }

    private boolean hasConfirmedRequest(Long userId, Long eventId, Throwable t) {
        throw new RuntimeException("Request service is temporarily unavailable. Please try again later.");
    }



    @CircuitBreaker(name = "request-service", fallbackMethod = "fallbackCountConfirmedRequestsByEventIds")
    public List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        return requestClient.countConfirmedRequestsByEventIds(eventIds);
    }


    private List<Object[]> fallbackCountConfirmedRequestsByEventIds(List<Long> eventIds, Throwable t) {
        throw new RuntimeException("Request service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "request-service", fallbackMethod = "fallbackCountConfirmedRequestsByEventId")
    public Long countConfirmedRequestsByEventId(Long id) {
        return requestClient.countConfirmedRequestsByEventId(id);
    }

    private Long fallbackCountConfirmedRequestsByEventId(Long id, Throwable t) {
        throw new RuntimeException("Request service is temporarily unavailable. Please try again later.");
    }

}

