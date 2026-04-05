package ru.practicum.category_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category_service.mapper.CategoryMapper;
import ru.practicum.category_service.repository.CategoryRepository;
import ru.practicum.interaction_api.dto.category.CategoryDto;
import ru.practicum.interaction_api.exception.NotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryInternalService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;


    public Map<Long, CategoryDto> findAllByIds(Set<Long> ids) {
        List<CategoryDto> list = categoryRepository.findAllById(ids).stream().map(categoryMapper::toCategoryDto).toList();
        Map<Long, CategoryDto> result = new HashMap<>(ids.size());
        for (CategoryDto categoryDto : list) {
            result.put(categoryDto.getId(), categoryDto);
        }
        return result;

    }

    public CategoryDto findById(Long id) {
        return categoryRepository.findById(id).map(categoryMapper::toCategoryDto).orElseThrow(() -> new NotFoundException("Категория  не найдена!"));
    }
}
