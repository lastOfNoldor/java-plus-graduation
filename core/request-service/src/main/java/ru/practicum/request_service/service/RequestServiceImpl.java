package ru.practicum.request_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.stats.CollectorClient;
import ru.practicum.interaction_api.dto.event.EventInternalDto;
import ru.practicum.interaction_api.enums.EventState;
import ru.practicum.interaction_api.enums.RequestStatus;
import ru.practicum.interaction_api.exception.ConflictException;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.request_service.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.param.CancelRequestParamDto;
import ru.practicum.request_service.dto.param.RequestParamDto;
import ru.practicum.request_service.dto.param.UpdateRequestStatusParamDto;
import ru.practicum.request_service.mapper.RequestMapper;
import ru.practicum.request_service.model.Request;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private static final String EVENT_NOT_FOUND = "Событие с ID %s не найдено";
    private static final String LIMIT_REACHED = "Достигнут лимит участников";

    private final RequestMapper requestMapper;
    private final RequestTransactionalService transactionalService;
    private final EventGatewayService eventGatewayService;
    private final CollectorClient collectorClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя с ID: {}", userId);
        return transactionalService.getUserRequests(userId);
    }

    @Override
    public ParticipationRequestDto createRequest(RequestParamDto paramDto) {
        log.info("Создание заявки пользователя {} на событие {}",
                paramDto.getUserId(), paramDto.getEventId());
        EventInternalDto eventById = eventGatewayService.findById(paramDto.getEventId());
        validateRequestCreation(paramDto.getUserId(), paramDto.getEventId(), eventById);
        Request request = createRequestEntity(eventById, paramDto.getUserId());
        Request savedRequest = transactionalService.save(request);

        if (savedRequest.getStatus() == RequestStatus.CONFIRMED) {
            collectorClient.sendRegister(paramDto.getUserId(), paramDto.getEventId());
        }

        log.info("Заявка создана с ID: {}", savedRequest.getId());
        return requestMapper.toDto(savedRequest);
    }


    private void validateRequestCreation(Long userId, Long eventId, EventInternalDto event) {
        if (transactionalService.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long confirmedRequests = transactionalService.countConfirmedRequestsByEventId(eventId);
        if (isParticipantLimitReached(event, confirmedRequests)) {
            throw new ConflictException(LIMIT_REACHED);
        }
    }

    private boolean isParticipantLimitReached(EventInternalDto event, long confirmedCount) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return false;
        }
        return confirmedCount >= event.getParticipantLimit();
    }

    private Request createRequestEntity(EventInternalDto eventById,Long userId) {
        Request request = requestMapper.toEntity(eventById.getId(), userId);

        if (isRequestModerationNotRequired(eventById) || eventById.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Заявка автоматически подтверждена (отключена модерация или лимит 0)");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Заявка создана со статусом PENDING");
        }

        return request;
    }

    @Override
    public ParticipationRequestDto cancelRequest(CancelRequestParamDto paramDto) {
        log.info("Отмена заявки {} пользователем {}", paramDto.getRequestId(), paramDto.getUserId());
        Request request = transactionalService.findByIdAndRequesterId(paramDto.getRequestId(), paramDto.getUserId());
        if (request.getStatus().equals(RequestStatus.CANCELED)) {
            throw new ConflictException("Заявка уже отменена");
        }

        request.setStatus(RequestStatus.CANCELED);
        Request updated = transactionalService.save(request);

        log.info("Заявка {} отменена пользователем {}", paramDto.getRequestId(), paramDto.getUserId());
        return requestMapper.toDto(updated);
    }


    @Override
    public List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto) {
        log.info("Получение заявок на событие {} пользователя {}", paramDto.getEventId(), paramDto.getUserId());
        if (!eventGatewayService.existsByIdAndInitiatorId(paramDto.getEventId(), paramDto.getUserId())) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, paramDto.getEventId()));
        }

        return transactionalService.getEventRequests(paramDto);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto) {
        log.info("Изменение статуса заявок на событие {} пользователем {}", paramDto.getEventId(), paramDto.getUserId());

        EventInternalDto eventDto = eventGatewayService.findByIdAndInitiatorId(paramDto.getEventId(), paramDto.getUserId());
        List<Request> requests = transactionalService.findByIdIn(paramDto.getUpdateRequest().getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены");
        }

        if (shouldAutoConfirmRequests(eventDto)) {
            return autoConfirmRequests(requests);
        }

        return processRequestsWithLimit(eventDto, requests, paramDto.getUpdateRequest().getStatus());
    }

    private boolean isRequestModerationNotRequired(EventInternalDto event) {
        return event.getRequestModeration() != null && !event.getRequestModeration();
    }

    private boolean shouldAutoConfirmRequests(EventInternalDto event) {
        return isRequestModerationNotRequired(event) || event.getParticipantLimit() == 0;
    }

    private EventRequestStatusUpdateResult autoConfirmRequests(List<Request> requests) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();

        for (Request request : requests) {
            if (request.getStatus().equals(RequestStatus.PENDING)) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(requestMapper.toDto(request));
            }
        }

        transactionalService.saveAll(requests);
        log.info("Все заявки автоматически подтверждены (отключена модерация или лимит 0)");

        return new EventRequestStatusUpdateResult(confirmed, List.of());
    }

    private EventRequestStatusUpdateResult processRequestsWithLimit(EventInternalDto event, List<Request> requests, RequestStatus targetStatus) {
        long confirmedRequests = transactionalService.countConfirmedRequestsByEventId(event.getId());
        long availableSlots = event.getParticipantLimit() - confirmedRequests;

        if (targetStatus == RequestStatus.CONFIRMED && availableSlots <= 0) {
            throw new ConflictException(LIMIT_REACHED);
        }

        return processEachRequest(requests, targetStatus, availableSlots);
    }

    private EventRequestStatusUpdateResult processEachRequest(List<Request> requests, RequestStatus targetStatus, long availableSlots) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            validateRequestStatus(request);
            availableSlots = updateRequestStatus(request, targetStatus, availableSlots, confirmed, rejected);
        }

        transactionalService.saveAll(requests);
        log.info("Статус заявок обновлён: подтверждено - {}, отклонено - {}", confirmed.size(), rejected.size());

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private void validateRequestStatus(Request request) {
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new ConflictException("Можно изменять только заявки в статусе PENDING" + ". Заявка ID: " + request.getId() + " имеет статус: " + request.getStatus());
        }
    }

    private long updateRequestStatus(Request request, RequestStatus targetStatus, long availableSlots, List<ParticipationRequestDto> confirmed, List<ParticipationRequestDto> rejected) {
        if (targetStatus == RequestStatus.CONFIRMED) {
            if (availableSlots > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(requestMapper.toDto(request));
                availableSlots--;
                log.debug("Заявка {} подтверждена", request.getId());
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toDto(request));
                log.debug("Заявка {} отклонена (лимит исчерпан)", request.getId());
            }
        } else if (targetStatus == RequestStatus.REJECTED) {
            request.setStatus(RequestStatus.REJECTED);
            rejected.add(requestMapper.toDto(request));
            log.debug("Заявка {} отклонена", request.getId());
        }

        return availableSlots;
    }



}