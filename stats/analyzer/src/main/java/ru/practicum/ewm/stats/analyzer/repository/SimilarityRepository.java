package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT es FROM EventSimilarity es WHERE es.event1 = :eventId OR es.event2 = :eventId ORDER BY es.similarity DESC")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    Optional<EventSimilarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("SELECT es FROM EventSimilarity es WHERE es.event1 IN :eventIds OR es.event2 IN :eventIds ORDER BY es.similarity DESC")
    List<EventSimilarity> findAllByEventIds(@Param("eventIds") Set<Long> eventIds);
}
