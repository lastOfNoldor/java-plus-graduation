package ru.practicum.stats_client;

import ru.practicum.stats_dto.EndpointHitDto;
import ru.practicum.stats_dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatClient {
    void hit(EndpointHitDto endpointHitDto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}