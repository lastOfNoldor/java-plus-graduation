package ru.practicum.event_service.event.mapper;

import org.mapstruct.*;
import ru.practicum.interaction_api.dto.category.CategoryDto;
import ru.practicum.interaction_api.dto.user.UserShortDto;
import ru.practicum.event_service.event.dto.EventFullDto;
import ru.practicum.event_service.event.dto.EventShortDto;
import ru.practicum.event_service.event.dto.NewEventDto;
import ru.practicum.event_service.event.dto.param.UpdateEventRequest;
import ru.practicum.event_service.event.model.Event;
import ru.practicum.interaction_api.enums.EventState;

import java.time.LocalDateTime;



@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {LocalDateTime.class, EventState.class})
public abstract class EventMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "annotation", source = "annotation")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "eventDate", source = "eventDate")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "paid", source = "paid")
    @Mapping(target = "participantLimit", source = "participantLimit")
    @Mapping(target = "requestModeration", source = "requestModeration")
    @Mapping(target = "categoryId", source = "categoryId")
    public abstract void updateEventFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "category", source = "category")
    public abstract EventFullDto toEventFullDto(Event event,UserShortDto initiator, CategoryDto category);

    public EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views, UserShortDto initiator, CategoryDto category) {
        EventFullDto dto = toEventFullDto(event,initiator,category);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }


    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "category", source = "category")
    public abstract EventShortDto toEventShortDto(Event event, UserShortDto initiator,  CategoryDto category);

    public EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views,  UserShortDto initiator,  CategoryDto category) {
        EventShortDto dto = toEventShortDto(event,initiator,category);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "initiatorId", source = "initiatorId")
    @Mapping(target = "publishedOn", ignore = true)
    public abstract Event toNewEvent(NewEventDto newEventDto, Long categoryId, Long initiatorId);

}
