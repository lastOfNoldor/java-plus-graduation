package ru.practicum.category_service.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import ru.practicum.category_service.dto.NewCategoryDto;
import ru.practicum.category_service.model.Category;
import ru.practicum.interaction_api.dto.category.CategoryDto;

@Component
public class CategoryMapper {

    public Category toCategory(NewCategoryDto newCategoryDto) {
        return Category.builder().name(newCategoryDto.getName()).build();
    }

    @Named("toCategoryDto")
    public CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }


}
