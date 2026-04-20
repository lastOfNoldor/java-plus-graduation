package ru.practicum.request_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.interaction_api.enums.RequestStatus;
import ru.practicum.request_service.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequesterId(Long userId);

    List<Request> findByEventId(Long eventId);

    List<Request> findByIdIn(List<Long> requestIds);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, RequestStatus status);


    @Query("SELECT COUNT(r) FROM Request r WHERE r.eventId = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long userId);

    @Query("SELECT r.eventId, COUNT(r) FROM Request r " +
            "WHERE r.eventId IN :eventIds AND r.status = :status " +
            "GROUP BY r.eventId")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds, @Param("status") RequestStatus status);

    Long countByEventIdAndStatus(Long id, RequestStatus requestStatus);
}