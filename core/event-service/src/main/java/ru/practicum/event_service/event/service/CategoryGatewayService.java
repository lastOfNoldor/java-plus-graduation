package ru.practicum.event_service.event.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction_api.contract.category_service.CategoryClient;
import ru.practicum.interaction_api.dto.category.CategoryDto;

import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class CategoryGatewayService {
    private final CategoryClient categoryClient;

    @CircuitBreaker(name = "category-service", fallbackMethod = "fallbackFindById")
    public CategoryDto findById(Long categoryId) {
        return categoryClient.findById(categoryId);
    }

    private CategoryDto fallbackFindById(Long categoryId, Throwable t) {
        throw new RuntimeException("Category service is temporarily unavailable. Please try again later.");
    }


    @CircuitBreaker(name = "category-service", fallbackMethod = "fallbackFindAllById")
    public Map<Long, CategoryDto> findAllById(Set<Long> categoryIds) {
        return categoryClient.findAllById(categoryIds);
    }

    private Map<Long, CategoryDto> fallbackFindAllById(Set<Long> categoryIds, Throwable t) {
        throw new RuntimeException("Category service is temporarily unavailable. Please try again later.");
    }

}
