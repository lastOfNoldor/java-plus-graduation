package ru.practicum.interaction_api.contract.request_service;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "request-service", path = "/internal/requests", configuration = RequestClientConfig.class)
public interface RequestClient extends RequestOperations {
}
