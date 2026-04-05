package ru.practicum.request_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction_api.enums.RequestStatus;
import ru.practicum.request_service.repository.RequestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalRequestService {
    private final RequestRepository requestRepository;


    public List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);
    }

    public Long countConfirmedRequestsByEventId(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }
}
