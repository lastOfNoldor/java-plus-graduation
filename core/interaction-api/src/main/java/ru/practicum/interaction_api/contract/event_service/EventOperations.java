package ru.practicum.interaction_api.contract.event_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction_api.dto.event.EventInternalDto;

public interface EventOperations {

    @GetMapping("/byId")
    EventInternalDto findById(@RequestParam Long eventId);

    @GetMapping("/existsByIdAndInitiatorId")
    boolean existsByIdAndInitiatorId(@RequestParam Long eventId, @RequestParam Long initiatorId);

    @GetMapping("/findByIdAndInitiatorId")
    EventInternalDto findByIdAndInitiatorId(@RequestParam Long eventId, @RequestParam Long initiatorId);

}
