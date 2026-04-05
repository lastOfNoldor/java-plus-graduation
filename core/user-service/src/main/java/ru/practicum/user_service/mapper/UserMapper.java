package ru.practicum.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.interaction_api.dto.user.UserShortDto;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequest newUserRequest);

    UserDto toUserDto(User user);

    UserShortDto toUserShortDto(User user);

}
