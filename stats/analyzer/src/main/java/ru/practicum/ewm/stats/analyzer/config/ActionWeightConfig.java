package ru.practicum.ewm.stats.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;


@Getter
@Setter
@Configuration
@ConfigurationProperties("analyzer.kafka.action-weight")
public class ActionWeightConfig {

    private double view;
    private double registered;
    private double like;

    public double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> view;
            case REGISTER -> registered;
            case LIKE -> like;
        };
    }
}
