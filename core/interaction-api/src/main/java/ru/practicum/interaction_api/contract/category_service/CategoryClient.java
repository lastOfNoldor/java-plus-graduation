package ru.practicum.interaction_api.contract.category_service;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "category-service", path = "/internal/categories", configuration = CategoryClientConfig.class)
public interface CategoryClient extends CategoryOperations {

}
