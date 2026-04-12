package ru.practicum.interaction_api.contract.request_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RequestOperations {

    @GetMapping("/countConfirmedByEventIds")
    List<Object[]> countConfirmedRequestsByEventIds(@RequestParam List<Long> eventIds);

    @GetMapping("/countConfirmedByEventId")
    Long countConfirmedRequestsByEventId(@RequestParam Long eventId);


}
