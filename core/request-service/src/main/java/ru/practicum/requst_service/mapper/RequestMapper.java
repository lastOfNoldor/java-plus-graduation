package ru.practicum.requst_service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.requst_service.dto.ParticipationRequestDto;
import ru.practicum.requst_service.model.Request;
import ru.practicum.interaction_api.enums.RequestStatus;

@Component
public class RequestMapper {

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder().id(request.getId()).created(request.getCreated()).event(request.getEventId()).requester(request.getRequesterId()).status(request.getStatus()).build();
    }

    public Request toEntity(Long eventId, Long requesterId) {
        return Request.builder().eventId(eventId).requesterId(requesterId).status(RequestStatus.PENDING).build();
    }
}