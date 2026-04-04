package ru.practicum.interaction_api.contract.event_service;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "event-service", path = "/internal/events", configuration = EventClientConfig.class)
public interface EventClient extends EventOperations {

}
