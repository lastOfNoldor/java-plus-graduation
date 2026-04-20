package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final SimilarityRepository similarityRepository;

    @Transactional
    public void process(EventSimilarityAvro avro) {
        long first = Math.min(avro.getEventA(), avro.getEventB());
        long second = Math.max(avro.getEventA(), avro.getEventB());
        Instant ts = avro.getTimestamp();

        similarityRepository.findByEvent1AndEvent2(first, second)
                .ifPresentOrElse(
                        existing -> {
                            existing.setSimilarity(avro.getScore());
                            existing.setTs(ts);
                            similarityRepository.save(existing);
                        },
                        () -> {
                            EventSimilarity similarity = new EventSimilarity();
                            similarity.setEvent1(first);
                            similarity.setEvent2(second);
                            similarity.setSimilarity(avro.getScore());
                            similarity.setTs(ts);
                            similarityRepository.save(similarity);
                        }
                );
    }
}
