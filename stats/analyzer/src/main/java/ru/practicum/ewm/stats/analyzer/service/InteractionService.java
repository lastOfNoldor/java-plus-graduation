package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.config.ActionWeightConfig;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;
import ru.practicum.ewm.stats.analyzer.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final ActionWeightConfig actionWeightConfig;

    @Transactional
    public void process(UserActionAvro action) {
        double newRating = actionWeightConfig.getWeight(action.getActionType());
        Instant ts = action.getTimestamp();

        interactionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .ifPresentOrElse(
                        existing -> {
                            if (newRating > existing.getRating()) {
                                existing.setRating(newRating);
                                existing.setTs(ts);
                                interactionRepository.save(existing);
                            }
                        },
                        () -> {
                            UserInteraction interaction = new UserInteraction();
                            interaction.setUserId(action.getUserId());
                            interaction.setEventId(action.getEventId());
                            interaction.setRating(newRating);
                            interaction.setTs(ts);
                            interactionRepository.save(interaction);
                        }
                );
    }
}
