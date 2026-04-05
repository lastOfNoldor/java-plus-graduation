package ru.practicum.interaction_api.contract.user_service;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", path = "/internal/users", configuration = UserClientConfig.class)
public interface UserClient extends UserOperations {

}
