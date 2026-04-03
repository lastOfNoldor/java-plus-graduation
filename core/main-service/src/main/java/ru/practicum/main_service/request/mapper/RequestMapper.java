package ru.practicum.main_service.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.request.dto.ParticipationRequestDto;
import ru.practicum.main_service.request.model.Request;
import ru.practicum.main_service.request.model.RequestStatus;

@Component
public class RequestMapper {

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder().id(request.getId()).created(request.getCreated()).event(request.getEvent().getId()).requester(request.getRequesterId()).status(request.getStatus()).build();
    }

    public Request toEntity(Event event, Long requesterId) {
        return Request.builder().event(event).requesterId(requesterId).status(RequestStatus.PENDING).build();
    }
}