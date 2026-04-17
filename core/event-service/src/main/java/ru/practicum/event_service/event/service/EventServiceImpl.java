package ru.practicum.event_service.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.event_service.event.dto.*;
import ru.practicum.event_service.event.dto.param.*;
import ru.practicum.event_service.event.mapper.EventMapper;
import ru.practicum.event_service.event.model.Event;
import ru.practicum.interaction_api.dto.category.CategoryDto;
import ru.practicum.interaction_api.dto.user.UserShortDto;
import ru.practicum.interaction_api.enums.EventState;
import ru.practicum.interaction_api.enums.StateAction;
import ru.practicum.interaction_api.exception.ConflictException;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.interaction_api.exception.ValidationException;
import ru.practicum.ewm.client.stats.StatClient;
import ru.practicum.stats_dto.EndpointHitDto;
import ru.practicum.stats_dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final UserGatewayService userGatewayService;
    private final CategoryGatewayService categoryGatewayService;
    private final RequestGatewayService requestGatewayService;
    private final EventMapper eventMapper;
    private final StatClient statClient;
    private final ModerationCommentService moderationCommentService;
    private final TransactionalEventService transactionalService;

    @Value("${event.moderation.page-size:10}")
    private int defaultModerationPageSize;

    @Value("${event.moderation.default-from:0}")
    private int defaultModerationFrom;

    @Override
    public List<EventShortDto> getEventsByUser(EventsByUserParams params) {
        log.debug("Получение событий пользователя: userId={}, from={}, size={}", params.getUserId(), params.getFrom(), params.getSize());
        Long userId = params.getUserId();
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        log.trace("Пагинация: offset={}, limit={}", pageable.getOffset(), pageable.getPageSize());
        List<Event> events = transactionalService.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) {
            log.trace("Пользователь userId={} не имеет событий", userId);
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsAndItsCountBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);
        log.trace("Статистика собрана: confirmedRequests={} записей, views={} записей", confirmedRequestsMap.size(), viewsMap.size());
        UserShortDto userData = getUserData(userId);
        Map<Long, CategoryDto> categories = getCategoryData(events);
        List<EventShortDto> result = events.stream().map(event -> {
            Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Long views = viewsMap.getOrDefault(event.getId(), 0L);
            CategoryDto categoryData = categories.get(event.getCategoryId());
            log.trace("Событие c id={}: confirmedRequests={}, views={}", event.getId(), confirmedRequests, views);
            return eventMapper.toEventShortDto(event, confirmedRequests, views, userData, categoryData);
        }).collect(Collectors.toList());
        log.debug("Возвращено {} EventShortDto для пользователя userId={}", result.size(), userId);
        return result;
    }

    private Map<Long, Long> getConfirmedRequestsAndItsCountBatch(List<Event> events) {
        log.trace("Получение подтвержденных запросов для {} событий", events.size());
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Object[]> results = requestGatewayService.countConfirmedRequestsByEventIds(eventIds);
        log.trace("Получено {} результатов о подтвержденных запросах", results.size());
        return results.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
    }

    private Map<Long, Long> getEventsViewsBatch(List<Event> events) {
        log.debug("Получение статистики просмотров для {} событий", events.size());
        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).collect(Collectors.toList());
        log.trace("Сформировано {} URI для запроса статистики", uris.size());
        LocalDateTime earliestCreated = events.stream().map(Event::getCreatedOn).min(LocalDateTime::compareTo).orElse(LocalDateTime.now().minusYears(1));
        log.trace("Диапазон запроса статистики: с {} по {}", earliestCreated, LocalDateTime.now());
        List<ViewStatsDto> stats = statClient.getStats(earliestCreated, LocalDateTime.now(), uris, false);
        log.debug("Получено {} записей статистики от внешнего сервиса", stats.size());
        return stats.stream().collect(Collectors.toMap(stat -> extractEventIdFromUri(stat.getUri()), ViewStatsDto::getHits, (existing, replacement) -> existing));
    }

    private Long extractEventIdFromUri(String uri) {
        String[] parts = uri.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        LocalDateTime now = LocalDateTime.now();

        if (newEventDto.getEventDate().isBefore(now)) {
            throw new ValidationException("Дата события не может быть в прошлом");
        }

        if (newEventDto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toNewEvent(newEventDto, newEventDto.getCategory(), userId);

        Event savedEvent = transactionalService.save(event);
        log.info("Создано новое событие с id: {}", savedEvent.getId());

        UserShortDto userData = getUserData(savedEvent.getInitiatorId());
        CategoryDto categoryData = getCategoryData(savedEvent.getCategoryId());

        return eventMapper.toEventFullDto(savedEvent, userData, categoryData);
    }

    private Long getEventRequests(Event event) {
        return requestGatewayService.countConfirmedRequestsByEventId(event.getId());

    }

    private Long getEventViews(Event event) {
        String uri = "/events/" + event.getId();
        List<ViewStatsDto> stats = statClient.getStats(event.getCreatedOn(), LocalDateTime.now(), List.of(uri), true);
        Long views = 0L;
        if (!stats.isEmpty()) {
            for (ViewStatsDto stat : stats) {
                if (stat.getUri().equals(uri)) {
                    views = stat.getHits();
                    break;
                }
            }
        }
        return views;
    }

    @Override
    public EventFullDto getEventByUser(EventByUserRequest request) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        Event event = transactionalService.findByIdAndInitiatorId(eventId,userId);
        Long views = getEventViews(event);
        Long eventRequests = getEventRequests(event);
        UserShortDto userData = getUserData(event.getInitiatorId());
        CategoryDto categoryDto = getCategoryData(event.getCategoryId());
        return eventMapper.toEventFullDto(event, eventRequests, views, userData, categoryDto);
    }

    private UserShortDto getUserData(Long id) {
        return userGatewayService.findById(id);
    }

    private Map<Long,UserShortDto> getUserData(List<Event> events) {
        Set<Long> request = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        return userGatewayService.findAllById(request);
    }

    private CategoryDto getCategoryData(Long id) {
        return categoryGatewayService.findById(id);
    }

    private Map<Long,CategoryDto> getCategoryData(List<Event> events) {
        Set<Long> request = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        return categoryGatewayService.findAllById(request);
    }

    public EventFullDto updateEventByUser(EventByUserRequest request, UpdateEventUserRequest updateEventRequest) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        Event event = transactionalService.findById(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не принадлежит пользователю");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (updateEventRequest.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEventRequest.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEventRequest.getEventDate().isBefore(now.plusHours(2))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }

        updateEventFields(event, updateEventRequest);
        StateAction state = updateEventRequest.getStateAction();
        if (state != null) {
            if (!state.isUserStateAction()) {
                throw new ValidationException("Передано не корректное действие");
            }
            switch (state) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    if (event.getState() == EventState.CANCELED) {
                        moderationCommentService.deleteCommentsByEventId(eventId);
                    }
                    event.setState(EventState.PENDING);
                    break;
            }
        }

        Event updatedEvent = transactionalService.save(event);
        log.info("Обновлено событие с id: {}", eventId);
        Long views = getEventViews(updatedEvent);
        Long eventRequests = getEventRequests(updatedEvent);
        UserShortDto userData = getUserData(updatedEvent.getInitiatorId());
        CategoryDto categoryData = getCategoryData(updatedEvent.getCategoryId());

        return eventMapper.toEventFullDto(updatedEvent, eventRequests, views, userData, categoryData);
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEvent) {
        Event event = transactionalService.findById(eventId);

        if (updateEvent.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEvent.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEvent.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 1 час от текущего момента");
            }
        }

        updateEventFields(event, updateEvent);
        StateAction state = updateEvent.getStateAction();
        if (state != null) {
            switch (state) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = transactionalService.save(event);
        log.info("Администратором обновлено событие с id: {}", eventId);
        Long views = getEventViews(updatedEvent);
        Long eventRequests = getEventRequests(event);
        UserShortDto userData = getUserData(updatedEvent.getInitiatorId());
        CategoryDto categoryData = getCategoryData(updatedEvent.getCategoryId());

        return eventMapper.toEventFullDto(updatedEvent, eventRequests, views, userData, categoryData);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventsByAdminParams params) {
        log.debug("Админский поиск событий: users={}, states={}, categories={}, from={}, size={}", params.getUsers(),
                params.getStates(), params.getCategories(), params.getFrom(), params.getSize());
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Long> users = params.getUsers();
        List<EventState> states = params.getStates();
        List<Long> categories = params.getCategories();
        log.trace("Критерии поиска: users={}, states={}, categories={}, диапазон=[{}, {}]", users, states, categories, rangeStart, rangeEnd);

        List<Event> events = transactionalService.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        log.debug("Найдено {} событий для администратора", events.size());
        if (events.isEmpty()) {
            log.trace("События не найдены по указанным критериям");
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsAndItsCountBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);
        Map<Long, UserShortDto> usersDto = getUserData(events);
        Map<Long, CategoryDto> categoriesDto = getCategoryData(events);
        log.trace("Статистика собрана: requests={} записей, views={} записей", confirmedRequestsMap.size(), viewsMap.size());
        return events.stream().map(event -> {
            Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Long views = viewsMap.getOrDefault(event.getId(), 0L);
            return eventMapper.toEventFullDto(event, confirmedRequests, views,usersDto.get(event.getInitiatorId()),categoriesDto.get(event.getCategoryId()) );
        }).collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getEventsPublic(EventsPublicParams params) {
        log.debug("Публичный поиск событий: sort={}, onlyAvailable={}, categories={}", params.getSort(),
                params.getOnlyAvailable(), params.getCategories());
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        String text = params.getText();
        Boolean paid = params.getPaid();
        Boolean onlyAvailable = params.getOnlyAvailable();
        List<Long> categories = params.getCategories();
        boolean sortedByViews = "views".equals(params.getSort());
        List<EventShortDto> result;
        if (sortedByViews) {
            log.debug("Сортировка по просмотрам (in-memory)");
            result = findSortedByViews(from, size, text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        } else {
            log.debug("Сортировка по дате (DB-level)");
            result = findSortedByDate(from, size, text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        }
        HttpServletRequest request = params.getRequest();
        sendStats(request);
        log.debug("Возвращено {} событий", result.size());
        return result;
    }

    private List<EventShortDto> findSortedByDate(int from, int size, String text, List<Long> categories,
                                                 Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 Boolean onlyAvailable) {
        Sort sorting = Sort.by("eventDate").ascending();
        Pageable pageable = PageRequest.of(from / size, size, sorting);
        log.trace("Поиск с пагинацией: offset={}, limit={}, onlyAvailable={}", pageable.getOffset(),
                pageable.getPageSize(), onlyAvailable);
        List<Event> events = transactionalService.findEventsPublic(text, categories, paid, rangeStart,
                rangeEnd, pageable);
        events = filterByOnlyAvailable(events, onlyAvailable);
        log.trace("Найдено {} событий (сортировка по дате)", events.size());
        return findSortedEventsPublicRequest(events);
    }

    private List<EventShortDto> findSortedByViews(int from, int size, String text, List<Long> categories,
                                                  Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable) {
        log.trace("Поиск без пагинации для сортировки по просмотрам, onlyAvailable={}", onlyAvailable);
        Pageable pageable = PageRequest.of(0, 1000);
        List<Event> events = transactionalService.findEventsPublic(text, categories, paid, rangeStart,
                rangeEnd,  pageable);
        log.trace("Найдено {} событий для сортировки по просмотрам", events.size());
        events = filterByOnlyAvailable(events, onlyAvailable);
        List<EventShortDto> result = findSortedEventsPublicRequest(events);
        if (result.size() > 100) {
            log.debug("Сортировка в памяти для {} событий (может быть затратно)", result.size());
        }
        result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        int toIndex = Math.min(from + size, result.size());
        if (from >= result.size()) {
            log.trace("Запрошенный offset превышает количество найденных событий");
            return Collections.emptyList();
        }
        List<EventShortDto> paginatedResult = result.subList(from, toIndex);
        log.trace("Применена пагинация in-memory: from={}, to={}, returned={}", from, toIndex, paginatedResult.size());

        return paginatedResult;
    }

    private List<Event> filterByOnlyAvailable(List<Event> events, Boolean onlyAvailable) {
        if (Boolean.TRUE.equals(onlyAvailable)) {
            Map<Long, Long> confirmedCounts = getConfirmedRequestsAndItsCountBatch(events);
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0
                            || confirmedCounts.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .toList();
        }
        return events;
    }

    private List<EventShortDto> findSortedEventsPublicRequest(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        if (events.size() > 20) {
            log.trace("Запрос статистики для {} событий (batch)", events.size());
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsAndItsCountBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);
        Map<Long, UserShortDto> userDtos = getUserData(events);
        Map<Long, CategoryDto> categoryDtos = getCategoryData(events);
        List<EventShortDto> result = events.stream().map(event -> {
            Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Long views = viewsMap.getOrDefault(event.getId(), 0L);
            UserShortDto userData = userDtos.get(event.getInitiatorId());
            CategoryDto categoryData = categoryDtos.get(event.getCategoryId());
            return eventMapper.toEventShortDto(event, confirmedRequests, views, userData,categoryData);
        }).collect(Collectors.toList());
        log.trace("Создано {} DTO", result.size());
        return result;
    }

    private void timeRangeValidation(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);
        if (rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Начальная дата не может быть позже конечной");
        }
    }

    private void sendStats(HttpServletRequest request) {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder().app("ewm-main-service").uri(request.getRequestURI())
                    .ip(request.getRemoteAddr()).timestamp(LocalDateTime.now().format(FORMATTER)).build();

            statClient.hit(hitDto);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());
        }
    }

    @Override
    public EventFullDto getEventPublic(Long eventId, HttpServletRequest request) {
        Event event = transactionalService.findById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }
        Long eventRequests = getEventRequests(event);
        Long views = getEventViews(event);
        UserShortDto userData = getUserData(event.getInitiatorId());
        CategoryDto categoryData = getCategoryData(event.getCategoryId());
        EventFullDto result = eventMapper.toEventFullDto(event, eventRequests, views, userData, categoryData);
        sendStats(request);

        return result;
    }


    private void updateEventFields(Event event, UpdateEventRequest updateEventRequest) {
        eventMapper.updateEventFromRequest(updateEventRequest, event);
    }

    public EventFullDtoWithModeration updateEventByAdminWithComment(
            Long eventId, UpdateEventAdminRequestWithComment updateRequest) {

        log.info("Обновление события с id: {} администратором с комментарием", eventId);

        Event event = transactionalService.findById(eventId);

        UpdateEventAdminRequest updateEvent = updateRequest.getUpdateEvent();
        String moderationComment = updateRequest.getModerationComment();

        if (updateEvent.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEvent.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEvent.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 1 час от текущего момента");
            }
        }

        updateEventFields(event, updateEvent);

        StateAction state = updateEvent.getStateAction();
        Long adminId = 1L;

        if (state != null) {
            switch (state) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());

                    if (moderationComment != null && !moderationComment.trim().isEmpty()) {
                        moderationCommentService.createComment(event, adminId, moderationComment);
                    }
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);

                    if (moderationComment == null || moderationComment.trim().isEmpty()) {
                        throw new ValidationException("При отклонении события необходимо указать причину");
                    }
                    moderationCommentService.createComment(event, adminId, moderationComment.trim());
                    break;
            }
        }

        Event updatedEvent = transactionalService.save(event);
        eventId = updatedEvent.getId();

        log.info("Администратором обновлено событие с id: {} с комментарием модерации", eventId);

        List<ModerationCommentDto> comments = moderationCommentService.getCommentsByEventId(eventId);

        Long views = getEventViews(updatedEvent);
        Long eventRequests = getEventRequests(updatedEvent);
        UserShortDto userData = getUserData(updatedEvent.getInitiatorId());
        CategoryDto categoryData = getCategoryData(updatedEvent.getCategoryId());

        EventFullDto eventFullDto = eventMapper.toEventFullDto(updatedEvent, eventRequests, views, userData, categoryData);

        return EventFullDtoWithModeration.fromEventFullDto(eventFullDto, comments);
    }

    @Override
    public List<EventFullDtoWithModeration> getEventsForModeration(Integer from, Integer size) {
        log.info("Получение событий для модерации, from={}, size={}", from, size);

        if (from == null) from = defaultModerationFrom;
        if (size == null) size = defaultModerationPageSize;

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").ascending());

        List<Event> events = transactionalService.findByState(EventState.PENDING, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsAndItsCountBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);
        Map<Long, UserShortDto> users = getUserData(events);
        Map<Long, CategoryDto> categories = getCategoryData(events);

        Map<Long, List<ModerationCommentDto>> commentsMap = getCommentsBatch(eventIds);

        return events.stream().map(event -> {
            Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Long views = viewsMap.getOrDefault(event.getId(), 0L);


            EventFullDto eventFullDto = eventMapper.toEventFullDto(event, confirmedRequests, views, users.get(event.getInitiatorId()), categories.get(event.getCategoryId()));

            List<ModerationCommentDto> comments = commentsMap.getOrDefault(event.getId(), Collections.emptyList());

            return EventFullDtoWithModeration.fromEventFullDto(eventFullDto, comments);
        }).collect(Collectors.toList());
    }

    private Map<Long, List<ModerationCommentDto>> getCommentsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ModerationCommentDto> allComments = moderationCommentService.getCommentsByEventIds(eventIds);

        return allComments.stream()
                .collect(Collectors.groupingBy(
                        ModerationCommentDto::getEventId,
                        Collectors.toList()
                ));
    }


}
