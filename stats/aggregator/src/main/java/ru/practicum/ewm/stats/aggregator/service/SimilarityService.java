package ru.practicum.ewm.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.aggregator.producer.EventSimilarityProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final EventSimilarityProducer producer;

    // Map<eventId, Map<userId, maxWeight>>
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    // Map<eventId, totalWeight>
    private final Map<Long, Double> eventTotalWeights = new HashMap<>();

    // Map<minEventId, Map<maxEventId, sMin>>
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    public void processAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = ACTION_WEIGHTS.get(action.getActionType());
        Instant timestamp = action.getTimestamp();

        double oldWeight = eventUserWeights
                .computeIfAbsent(eventId, e -> new HashMap<>())
                .getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            log.debug("Вес не изменился для userId={} eventId={}, пересчёт не требуется", userId, eventId);
            return;
        }

        eventUserWeights.get(eventId).put(userId, newWeight);

        double weightDiff = newWeight - oldWeight;
        eventTotalWeights.merge(eventId, weightDiff, Double::sum);

        for (long otherEventId : eventUserWeights.keySet()) {
            if (otherEventId == eventId) continue;

            double userWeightForOther = eventUserWeights
                    .getOrDefault(otherEventId, Map.of())
                    .getOrDefault(userId, 0.0);

            if (userWeightForOther == 0.0) continue;

            double oldMin = getMinWeightSum(eventId, otherEventId);
            double newMin = oldMin - Math.min(oldWeight, userWeightForOther)
                    + Math.min(newWeight, userWeightForOther);
            putMinWeightSum(eventId, otherEventId, newMin);

            double totalA = eventTotalWeights.getOrDefault(eventId, 0.0);
            double totalB = eventTotalWeights.getOrDefault(otherEventId, 0.0);

            if (totalA == 0.0 || totalB == 0.0) continue;

            double similarity = newMin / (Math.sqrt(totalA) * Math.sqrt(totalB));

            // упорядочиваем пару перед отправкой
            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            producer.send(first, second, similarity, timestamp);
        }
    }

    private double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private void putMinWeightSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }
}