package ru.practicum.ewm.stats.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.*;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class RecommendationsClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public List<RecommendedEventProto> getRecommendations(long userId, int maxResults) {
        try {
            UserRecommendationsRequestProto request = UserRecommendationsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
            return toList(iterator);
        } catch (Exception e) {
            log.warn("Не удалось получить рекомендации от Analyzer: {}", e.getMessage());
            return List.of();
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
            return toList(iterator);
        } catch (Exception e) {
            log.warn("Не удалось получить похожие мероприятия от Analyzer: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
            Map<Long, Double> result = new HashMap<>();
            iterator.forEachRemaining(event ->
                    result.put(event.getEventId(), event.getScore())
            );
            return result;
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги от Analyzer: {}", e.getMessage());
            return Map.of();
        }
    }

    private List<RecommendedEventProto> toList(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).toList();
    }
}


