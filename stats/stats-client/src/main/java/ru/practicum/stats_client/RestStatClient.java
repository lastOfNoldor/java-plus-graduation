package ru.practicum.stats_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats_dto.EndpointHitDto;
import ru.practicum.stats_dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
public class RestStatClient implements StatClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public RestStatClient(DiscoveryClient discoveryClient,
                          @Value("${discovery.services.stats-server-id}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = new RetryTemplate();
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            URI uri = makeUri("/hit");
            RestClient.builder().build()
                    .post()
                    .uri(uri)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Статистика успешно отправлена: app={}, uri={}",
                    endpointHitDto.getApp(), endpointHitDto.getUri());

        } catch (Exception e) {
            log.error("Не удалось отправить статистику: app={}, uri={}, ошибка: {}",
                    endpointHitDto.getApp(), endpointHitDto.getUri(), e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        try {
            URI baseUri = makeUri("/stats");

            UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri)
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER));

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }
            if (unique != null) {
                builder.queryParam("unique", unique);
            }

            ResponseEntity<List<ViewStatsDto>> response = RestClient.builder().build()
                    .get()
                    .uri(builder.build().toUri())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});

            List<ViewStatsDto> stats = response.getBody() != null ?
                    response.getBody() : Collections.emptyList();
            log.debug("Статистика успешно получена: период с {} по {}, количество записей: {}",
                    start, end, stats.size());

            return stats;

        } catch (Exception e) {
            log.error("Не удалось получить статистику: период с {} по {}, ошибка: {}",
                    start, end, e.getMessage());
            return Collections.emptyList();
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient.getInstances(statsServiceId).getFirst();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}