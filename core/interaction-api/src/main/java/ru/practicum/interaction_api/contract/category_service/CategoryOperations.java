package ru.practicum.interaction_api.contract.category_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction_api.dto.category.CategoryDto;


import java.util.Map;
import java.util.Set;

public interface CategoryOperations {

    @GetMapping("/allById")
    Map<Long, CategoryDto> findAllById(@RequestParam Set<Long> ids);

    @GetMapping("/byId")
    CategoryDto findById(@RequestParam Long id);

}
