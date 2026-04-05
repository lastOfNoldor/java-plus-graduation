package ru.practicum.request_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.param.RequestParamDto;
import ru.practicum.request_service.mapper.RequestMapper;
import ru.practicum.request_service.model.Request;
import ru.practicum.request_service.repository.RequestRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestTransactionalService {
    private static final String REQUEST_NOT_FOUND = "Событие с ID %s не найдено";
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;


    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя с ID: {}", userId);
        return requestRepository.findByRequesterId(userId).stream().map(requestMapper::toDto).toList();
    }

    @Transactional
    public Request save(Request request) {
        return requestRepository.save(request);
    }

    public boolean existsByRequesterIdAndEventId(Long userId, Long eventId){
        return requestRepository.existsByRequesterIdAndEventId(userId, eventId);
    }

    public Request findByIdAndRequesterId(Long requestId, Long userId){
        return requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(REQUEST_NOT_FOUND, requestId)));
    }

    public List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto) {
        return requestRepository.findByEventId(paramDto.getEventId()).stream().map(requestMapper::toDto).toList();
    }

    @Transactional
    public List<Request> findByIdIn(List<Long> requestIds) {
        return requestRepository.findByIdIn(requestIds);
    }

    public Long countConfirmedRequestsByEventId(Long id){
        return requestRepository.countConfirmedRequestsByEventId(id);
    }

    @Transactional
    public void saveAll(List<Request> requests) {
        requestRepository.saveAll(requests);
    }


}
