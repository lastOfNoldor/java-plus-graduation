package ru.practicum.ewm.stats.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    private static final String TOPIC = "stats.user-actions.v1";

    public void send(UserActionAvro action) {
        kafkaTemplate.send(TOPIC, action.getUserId(), action);
    }
}
