package ru.practicum.ewm.stats.analyzer.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.config.KafkaConfig;
import ru.practicum.ewm.stats.analyzer.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    private final SimilarityService similarityService;
    private final KafkaConfig kafkaConfig;
    private volatile boolean running = true;
    private KafkaConsumer<String, EventSimilarityAvro> consumer;

    @PostConstruct
    public void start() {
        Properties props = kafkaConfig.buildProperties("EventSimilarityProcessor");
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(kafkaConfig.getTopics("EventSimilarityProcessor"));

        Thread thread = new Thread(this::pollLoop);
        thread.setName("event-similarity-processor");
        thread.start();
    }

    private void pollLoop() {
        Duration pollTimeout = kafkaConfig.getPollTimeout("EventSimilarityProcessor");
        try {
            while (running) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(pollTimeout);

                try {
                    for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                        similarityService.process(record.value());
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("Ошибка обработки батча", e);
                }
            }
        } catch (WakeupException e) {
            log.info("EventSimilarityProcessor получил сигнал остановки");
        } finally {
            consumer.close();
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        consumer.wakeup();
    }
}
