package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;
import ru.practicum.ewm.stats.analyzer.repository.InteractionRepository;
import ru.practicum.ewm.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        return eventIds.stream()
                .map(eventId -> {
                    Double sum = interactionRepository.sumRatingByEventId(eventId);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(sum != null ? sum : 0.0)
                            .build();
                })
                .toList();
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> viewedEvents = interactionRepository.findEventIdsByUserId(userId);

        return similarityRepository.findAllByEventId(eventId)
                .stream()
                .filter(es -> {
                    long other = es.getEvent1() == eventId ? es.getEvent2() : es.getEvent1();
                    return !viewedEvents.contains(other);
                })
                .sorted(Comparator.comparingDouble(EventSimilarity::getSimilarity).reversed())
                .limit(maxResults)
                .map(es -> {
                    long other = es.getEvent1() == eventId ? es.getEvent2() : es.getEvent1();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(other)
                            .setScore(es.getSimilarity())
                            .build();
                })
                .toList();
    }

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        List<UserInteraction> recentInteractions = interactionRepository
                .findTopNByUserIdOrderByTsDesc(userId, PageRequest.of(0, maxResults));

        if (recentInteractions.isEmpty()) {
            log.debug("Пользователь {} ещё не взаимодействовал ни с одним мероприятием", userId);
            return List.of();
        }

        Set<Long> viewedEventIds = recentInteractions.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserInteraction interaction : recentInteractions) {
            similarityRepository.findAllByEventId(interaction.getEventId())
                    .stream()
                    .filter(es -> {
                        long other = es.getEvent1() == interaction.getEventId()
                                ? es.getEvent2() : es.getEvent1();
                        return !viewedEventIds.contains(other);
                    })
                    .forEach(es -> {
                        long other = es.getEvent1() == interaction.getEventId()
                                ? es.getEvent2() : es.getEvent1();
                        candidateScores.merge(other, es.getSimilarity(), Double::sum);
                    });
        }

        if (candidateScores.isEmpty()) {
            log.debug("Не найдено новых мероприятий для пользователя {}", userId);
            return List.of();
        }

        List<Long> topCandidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        return topCandidates.stream()
                .map(candidateId -> {
                    double score = predictScore(candidateId, userId, viewedEventIds, maxResults);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(candidateId)
                            .setScore(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .toList();
    }

    private double predictScore(long candidateId, long userId,
                                Set<Long> viewedEventIds, int maxResults) {
        List<EventSimilarity> kNearest = similarityRepository.findTopSimilarViewedEvents(
                candidateId, viewedEventIds, PageRequest.of(0, maxResults)
        );

        if (kNearest.isEmpty()) return 0.0;

        Set<Long> neighborIds = kNearest.stream()
                .map(es -> es.getEvent1() == candidateId ? es.getEvent2() : es.getEvent1())
                .collect(Collectors.toSet());

        Map<Long, Double> ratings = interactionRepository
                .findRatingsByUserIdAndEventIds(userId, neighborIds);

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity es : kNearest) {
            long neighborId = es.getEvent1() == candidateId
                    ? es.getEvent2() : es.getEvent1();
            double similarity = es.getSimilarity();
            double userRating = ratings.getOrDefault(neighborId, 0.0);

            weightedSum += similarity * userRating;
            similaritySum += similarity;
        }

        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }
}
