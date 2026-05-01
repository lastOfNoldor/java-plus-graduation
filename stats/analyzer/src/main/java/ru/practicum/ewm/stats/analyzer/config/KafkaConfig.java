package ru.practicum.ewm.stats.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Configuration
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfig {

    private Map<String, String> commonProperties;
    private List<KafkaConsumerProperties> consumers;

    @Getter
    @Setter
    public static class KafkaConsumerProperties {
        private String type;
        private Map<String, String> properties;
        private List<String> topics;
        private Duration pollTimeout;
    }

    public Properties buildProperties(String type) {
        Properties props = new Properties();
        props.putAll(commonProperties);
        consumers.stream()
                .filter(c -> c.getType().equals(type))
                .findFirst()
                .ifPresent(c -> props.putAll(c.getProperties()));
        return props;
    }

    public List<String> getTopics(String type) {
        return consumers.stream()
                .filter(c -> c.getType().equals(type))
                .findFirst()
                .map(KafkaConsumerProperties::getTopics)
                .orElse(List.of());
    }

    public Duration getPollTimeout(String type) {
        return consumers.stream()
                .filter(c -> c.getType().equals(type))
                .findFirst()
                .map(KafkaConsumerProperties::getPollTimeout)
                .orElse(Duration.ofMillis(500));
    }
}