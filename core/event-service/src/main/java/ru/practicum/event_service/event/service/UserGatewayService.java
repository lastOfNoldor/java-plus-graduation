package ru.practicum.event_service.event.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.practicum.interaction_api.contract.user_service.UserClient;
import ru.practicum.interaction_api.dto.user.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserGatewayService {
    private final UserClient userClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackFindById")
    public UserShortDto findById(Long userId) {
        return userClient.findById(userId);
    }

    private UserShortDto fallbackFindById(List<Long> userIds) {
        throw new RuntimeException("User service is temporarily unavailable. Please try again later.");
    }


    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackFindAllById")
    public Map<Long, UserShortDto> findAllById(Set<Long> userIds) {
        return userClient.findAllById(userIds);
    }

    private Map<Long, UserShortDto> fallbackFindAllById(List<Long> userIds) {
        throw new RuntimeException("User service is temporarily unavailable. Please try again later.");
    }

}
