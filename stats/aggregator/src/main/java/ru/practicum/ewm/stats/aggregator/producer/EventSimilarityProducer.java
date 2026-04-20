package ru.practicum.ewm.stats.aggregator.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class EventSimilarityProducer {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${spring.kafka.topics.events-similarity}")
    private String similarityTopic;

    public void send(long eventA, long eventB, double score, Instant timestamp) {
        EventSimilarityAvro event = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();

        String key = eventA + ":" + eventB;
        kafkaTemplate.send(similarityTopic, key, event);
    }
}
