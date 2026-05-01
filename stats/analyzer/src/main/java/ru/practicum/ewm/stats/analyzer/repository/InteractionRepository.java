package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface InteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findTopNByUserIdOrderByTsDesc(Long userId, Pageable pageable);

    @Query("SELECT ui.eventId FROM UserInteraction ui WHERE ui.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ui.eventId, ui.rating FROM UserInteraction ui " +
            "WHERE ui.userId = :userId AND ui.eventId IN :eventIds")
    List<Object[]> findRatingsByUserIdAndEventIdsRaw(@Param("userId") Long userId,
                                                     @Param("eventIds") Set<Long> eventIds);

    default Map<Long, Double> findRatingsByUserIdAndEventIds(Long userId, Set<Long> eventIds) {
        return findRatingsByUserIdAndEventIdsRaw(userId, eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }

    @Query("SELECT ui.eventId, SUM(ui.rating) FROM UserInteraction ui WHERE ui.eventId IN :eventIds GROUP BY ui.eventId")
    List<Object[]> sumRatingsByEventIdsRaw(@Param("eventIds") List<Long> eventIds);

    default Map<Long, Double> sumRatingsByEventIds(List<Long> eventIds) {
        return sumRatingsByEventIdsRaw(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }
}
