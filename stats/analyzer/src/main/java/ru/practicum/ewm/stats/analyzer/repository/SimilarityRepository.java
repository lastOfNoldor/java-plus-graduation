package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    // все похожие мероприятия для данного (event1 или event2)
    @Query("SELECT es FROM EventSimilarity es WHERE es.event1 = :eventId OR es.event2 = :eventId ORDER BY es.similarity DESC")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    // K ближайших соседей среди просмотренных пользователем
    @Query("""
            SELECT es FROM EventSimilarity es
            WHERE (es.event1 = :eventId OR es.event2 = :eventId)
            AND (es.event1 IN :viewedEvents OR es.event2 IN :viewedEvents)
            ORDER BY es.similarity DESC
            """)
    List<EventSimilarity> findTopSimilarViewedEvents(@Param("eventId") Long eventId,
                                                     @Param("viewedEvents") Set<Long> viewedEvents,
                                                     Pageable pageable);

    Optional<EventSimilarity> findByEvent1AndEvent2(Long event1, Long event2);
}
