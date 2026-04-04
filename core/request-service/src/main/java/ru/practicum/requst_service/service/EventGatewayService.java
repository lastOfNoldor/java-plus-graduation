package ru.practicum.requst_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction_api.contract.event_service.EventClient;
import ru.practicum.interaction_api.dto.event.EventInternalDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventGatewayService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "event-service", fallbackMethod = "fallbackFindById")
    public EventInternalDto findById(Long eventId) {
        return eventClient.findById(eventId);
    }

    private EventInternalDto fallbackFindById(Long eventId) {
        throw new RuntimeException("Event service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "event-service", fallbackMethod = "fallbackExistsByIdAndInitiatorId")
    public boolean existsByIdAndInitiatorId(Long eventId,Long initiatorId) {
        return eventClient.existsByIdAndInitiatorId(eventId, initiatorId);
    }

    private boolean fallbackExistsByIdAndInitiatorId(Long eventId,Long initiatorId) {
        return false;
    }

    @CircuitBreaker(name = "event-service", fallbackMethod = "fallbackFindByIdAndInitiatorId")
    public EventInternalDto findByIdAndInitiatorId(Long eventId,Long initiatorId) {
        return eventClient.findByIdAndInitiatorId(eventId, initiatorId);
    }

    private EventInternalDto fallbackFindByIdAndInitiatorId(Long eventId,Long initiatorId) {
        throw new RuntimeException("Event service is temporarily unavailable. Please try again later.");
    }


}
