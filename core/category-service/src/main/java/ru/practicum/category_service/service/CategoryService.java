package ru.practicum.category_service.service;

import ru.practicum.category_service.dto.NewCategoryDto;
import ru.practicum.category_service.model.Category;
import ru.practicum.interaction_api.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategoryById(Long catId);

    Category getEntityById(Long catId);

}
