package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface InteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    // последние N мероприятий пользователя по дате
    List<UserInteraction> findTopNByUserIdOrderByTsDesc(Long userId, Pageable pageable);

    // все мероприятия с которыми взаимодействовал пользователь
    @Query("SELECT ui.eventId FROM UserInteraction ui WHERE ui.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    // сумма максимальных весов всех пользователей для мероприятия
    @Query("SELECT SUM(ui.rating) FROM UserInteraction ui WHERE ui.eventId = :eventId")
    Double sumRatingByEventId(@Param("eventId") Long eventId);

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
}
