package ru.practicum.user_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction_api.contract.user_service.UserOperations;
import ru.practicum.interaction_api.dto.user.UserShortDto;
import ru.practicum.user_service.service.UserInternalService;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/internal/users")
public class UserInternalController implements UserOperations {
    private final UserInternalService userInternalService;

    @Override
    public Map<Long, UserShortDto> findAllById(Set<Long> ids) {
        return userInternalService.findAllByIds(ids);
    }

    @Override
    public UserShortDto findById(Long id) {
        return userInternalService.findById(id);
    }


}
