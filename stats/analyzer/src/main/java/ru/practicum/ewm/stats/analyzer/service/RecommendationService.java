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
        Map<Long, Double> ratingsMap = interactionRepository.sumRatingsByEventIds(eventIds);

        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(ratingsMap.getOrDefault(eventId, 0.0))
                        .build())
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

        List<EventSimilarity> allSimilarities = similarityRepository.findAllByEventIds(viewedEventIds);

        Map<Long, Double> candidateScores = new HashMap<>();

        for (EventSimilarity es : allSimilarities) {
            long viewed = viewedEventIds.contains(es.getEvent1()) ? es.getEvent1() : es.getEvent2();
            long candidate = es.getEvent1() == viewed ? es.getEvent2() : es.getEvent1();

            if (!viewedEventIds.contains(candidate)) {
                candidateScores.merge(candidate, es.getSimilarity(), Double::sum);
            }
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

        Set<Long> topCandidatesSet = new HashSet<>(topCandidates);

        List<EventSimilarity> candidateSimilarities = similarityRepository
                .findAllByEventIds(topCandidatesSet);

        Map<Long, List<EventSimilarity>> similaritiesByCandidate = new HashMap<>();
        for (EventSimilarity es : candidateSimilarities) {
            long candidate = topCandidatesSet.contains(es.getEvent1()) ? es.getEvent1() : es.getEvent2();
            long neighbor = es.getEvent1() == candidate ? es.getEvent2() : es.getEvent1();

            if (viewedEventIds.contains(neighbor)) {
                similaritiesByCandidate
                        .computeIfAbsent(candidate, k -> new ArrayList<>())
                        .add(es);
            }
        }

        Map<Long, Double> userRatings = interactionRepository
                .findRatingsByUserIdAndEventIds(userId, viewedEventIds);

        return topCandidates.stream()
                .map(candidateId -> {
                    double score = predictScore(
                            candidateId,
                            similaritiesByCandidate.getOrDefault(candidateId, List.of()),
                            userRatings,
                            maxResults
                    );
                    return RecommendedEventProto.newBuilder()
                            .setEventId(candidateId)
                            .setScore(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .toList();
    }

    private double predictScore(long candidateId,
                                List<EventSimilarity> kNearest,
                                Map<Long, Double> userRatings,
                                int maxResults) {
        if (kNearest.isEmpty()) return 0.0;

        List<EventSimilarity> topK = kNearest.stream()
                .sorted(Comparator.comparingDouble(EventSimilarity::getSimilarity).reversed())
                .limit(maxResults)
                .toList();

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity es : topK) {
            long neighborId = es.getEvent1() == candidateId
                    ? es.getEvent2() : es.getEvent1();
            double similarity = es.getSimilarity();
            double userRating = userRatings.getOrDefault(neighborId, 0.0);

            weightedSum += similarity * userRating;
            similaritySum += similarity;
        }

        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }
}