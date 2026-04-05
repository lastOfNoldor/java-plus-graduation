package ru.practicum.event_service.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event_service.event.model.Event;
import ru.practicum.event_service.event.repository.EventRepository;
import ru.practicum.interaction_api.enums.EventState;
import ru.practicum.interaction_api.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionalEventService {
    private final EventRepository eventRepository;
    private static final String EVENT_NOT_FOUND = "Событие с ID %s не найдено";


    public List<Event> findByInitiatorId(Long userId, Pageable pageable) {
        return eventRepository.findByInitiatorId(userId, pageable);
    }

    @Transactional
    public Event save(Event event) {
        return eventRepository.save(event);
    }

    public Event findByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
    }

    public Event findById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
    }

    public List<Event> findEventsByAdmin(List<Long> users,
                                  List<EventState> states,
                                  List<Long> categories,
                                  LocalDateTime rangeStart,
                                  LocalDateTime rangeEnd,
                                  Pageable pageable) {
        return eventRepository.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
    }

    public List<Event> findEventsPublic(String text,
                                 List<Long> categories,
                                 Boolean paid,
                                 LocalDateTime rangeStart,
                                 LocalDateTime rangeEnd,
                                 Boolean onlyAvailable,
                                 Pageable pageable) {
        return eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);
    }

    public List<Event> findByState(EventState eventState, Pageable pageable){
        return eventRepository.findByState(eventState, pageable);
    }

}
