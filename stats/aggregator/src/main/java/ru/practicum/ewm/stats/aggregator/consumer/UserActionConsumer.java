package ru.practicum.ewm.stats.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.aggregator.service.SimilarityService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityService similarityService;

    @Value("${spring.kafka.topics.user-actions}")
    private String userActionsTopic;

    @KafkaListener(
            topics = "${spring.kafka.topics.user-actions}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(UserActionAvro action) {
        try {
            similarityService.processAction(action);
        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
        }
    }
}
