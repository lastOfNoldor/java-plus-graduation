package ru.practicum.main_service.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction_api.contract.event_service.EventOperations;
import ru.practicum.interaction_api.dto.event.EventInternalDto;
import ru.practicum.main_service.event.service.EventInternalService;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/internal/events")
public class InternalEventController implements EventOperations {
    private final EventInternalService eventService;

    @Override
    public EventInternalDto findById(@RequestParam Long eventId) {
        return eventService.findById(eventId);
    }

    @Override
    public boolean existsByIdAndInitiatorId(@RequestParam Long eventId, @RequestParam Long initiatorId) {
        return eventService.existsByIdAndInitiatorId(eventId, initiatorId);
    }

    @Override
    public EventInternalDto findByIdAndInitiatorId(@RequestParam Long eventId,@RequestParam Long initiatorId) {
        return eventService.findByIdAndInitiatorId(eventId, initiatorId);
    }


}
