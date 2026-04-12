package ru.practicum.category_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.category_service.service.CategoryInternalService;
import ru.practicum.interaction_api.contract.category_service.CategoryOperations;
import ru.practicum.interaction_api.dto.category.CategoryDto;

import java.util.Map;
import java.util.Set;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/internal/categories")
public class CategoryInternalController implements CategoryOperations {
    private final CategoryInternalService internalService;

    @Override
    public Map<Long, CategoryDto> findAllById(Set<Long> ids) {
        return internalService.findAllByIds(ids);
    }

    @Override
    public CategoryDto findById(Long id) {
        return internalService.findById(id);
    }
}
