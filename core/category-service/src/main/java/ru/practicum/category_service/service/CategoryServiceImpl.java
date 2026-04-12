package ru.practicum.category_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.category_service.dto.NewCategoryDto;
import ru.practicum.category_service.mapper.CategoryMapper;
import ru.practicum.category_service.model.Category;
import ru.practicum.interaction_api.dto.category.CategoryDto;
import ru.practicum.interaction_api.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final EventGatewayService eventGatewayService;
    private final CategoryTransactionalService transactionalService;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {

        if (transactionalService.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с именем '" + newCategoryDto.getName() + "' уже существует");
        }

        Category category = categoryMapper.toCategory(newCategoryDto);
        Category savedCategory = transactionalService.saveEntity(category);

        log.info("Создана новая категория с id: {}", savedCategory.getId());
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public void deleteCategory(Long catId) {
        if (eventGatewayService.existsByCategoryId(catId)) {
            throw new ConflictException("Невозможно удалить категорию: существуют связанные события");
        }
        transactionalService.deleteById(catId);
        log.info("Удалена категория с id: {}", catId);
    }

    @Override
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = getEntityById(catId);

        if (transactionalService.existsByNameAndIdNot(categoryDto.getName(), catId)) {
            throw new ConflictException("Категория с именем '" + categoryDto.getName() + "' уже существует");
        }

        category.setName(categoryDto.getName());
        Category updatedCategory = transactionalService.saveEntity(category);

        log.info("Обновлена категория с id: {}", catId);
        return categoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Category> categories = transactionalService.findAll(pageable).getContent();

        return categories.stream().map(categoryMapper::toCategoryDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = getEntityById(catId);
        return categoryMapper.toCategoryDto(category);
    }

    public Category getEntityById(Long catId) {
        return transactionalService.findById(catId);
    }

}
