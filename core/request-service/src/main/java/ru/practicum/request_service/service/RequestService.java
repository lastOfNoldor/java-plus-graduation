package ru.practicum.request_service.service;

import ru.practicum.request_service.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.param.CancelRequestParamDto;
import ru.practicum.request_service.dto.param.RequestParamDto;
import ru.practicum.request_service.dto.param.UpdateRequestStatusParamDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(RequestParamDto paramDto);

    ParticipationRequestDto cancelRequest(CancelRequestParamDto paramDto);

    List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto);

    EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto);

}