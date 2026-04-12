package ru.practicum.interaction_api.contract.user_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction_api.dto.user.UserShortDto;

import java.util.Map;
import java.util.Set;

public interface UserOperations {

    @GetMapping("/allById")
    Map<Long, UserShortDto> findAllById(@RequestParam Set<Long> ids);

    @GetMapping("/byId")
    UserShortDto findById(@RequestParam Long id);

}
