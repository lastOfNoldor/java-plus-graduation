package ru.practicum.request_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction_api.contract.request_service.RequestOperations;
import ru.practicum.request_service.service.InternalRequestService;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/internal/requests")
public class InternalRequestController implements RequestOperations {
    private final InternalRequestService internalRequestService;

    @Override
    public List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        return internalRequestService.countConfirmedRequestsByEventIds(eventIds);
    }


    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        return internalRequestService.countConfirmedRequestsByEventId(eventId);

    }
}
