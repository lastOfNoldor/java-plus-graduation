package ru.practicum.main_service.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction_api.dto.event.EventInternalDto;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.main_service.category.service.CategoryService;
import ru.practicum.main_service.event.mapper.EventMapper;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.repository.EventRepository;
import ru.practicum.requst_service.repository.RequestRepository;
import ru.practicum.stats_client.StatClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventInternalService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final StatClient statClient;
    private final ModerationCommentService moderationCommentService;
    private static final String EVENT_NOT_FOUND = "Событие с ID %s не найдено";


    public EventInternalDto findById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState()).build();
    }

    public boolean existsByIdAndInitiatorId(Long eventId,Long initiatorId) {
        return eventRepository.existsByIdAndInitiatorId(eventId, initiatorId);
    }

    public EventInternalDto findByIdAndInitiatorId(Long eventId,Long initiatorId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState()).build();
    }
}
