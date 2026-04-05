package ru.practicum.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction_api.dto.user.UserShortDto;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.user_service.mapper.UserMapper;
import ru.practicum.user_service.model.User;
import ru.practicum.user_service.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInternalService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    public Map<Long, UserShortDto> findAllByIds(Set<Long> ids) {
        List<UserShortDto> list = userRepository.findAllById(ids).stream().map(userMapper::toUserShortDto).toList();
        Map<Long, UserShortDto> result = new HashMap<>(ids.size());
        for (UserShortDto userShortDto : list) {
            result.put(userShortDto.getId(), userShortDto);
        }
        return result;

    }

    public UserShortDto findById(Long id) {
        return userRepository.findById(id).map(userMapper::toUserShortDto).orElseThrow(() -> new NotFoundException("Пользователь не найден!"));
    }
}
