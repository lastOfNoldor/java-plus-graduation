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
import ru.practicum.ewm.stats.analyzer.service.InteractionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionsProcessor {

    private final InteractionService interactionService;
    private final KafkaConfig kafkaConfig;
    private volatile boolean running = true;
    private KafkaConsumer<Long, UserActionAvro> consumer;

    @PostConstruct
    public void start() {
        Properties props = kafkaConfig.buildProperties("UserActionsProcessor");
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(kafkaConfig.getTopics("UserActionsProcessor"));

        Thread thread = new Thread(this::pollLoop);
        thread.setName("user-actions-processor");
        thread.start();
    }

    private void pollLoop() {
        Duration pollTimeout = kafkaConfig.getPollTimeout("UserActionsProcessor");
        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(pollTimeout);
                try {
                    for (ConsumerRecord<Long, UserActionAvro> record : records) {
                        interactionService.process(record.value());
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("Ошибка обработки батча", e);
                }
            }
        } catch (WakeupException e) {
            log.info("UserActionsProcessor получил сигнал остановки");
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
