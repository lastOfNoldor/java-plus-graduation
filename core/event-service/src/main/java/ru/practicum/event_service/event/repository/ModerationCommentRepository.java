package ru.practicum.event_service.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.event_service.event.model.ModerationComment;

import java.util.List;


public interface ModerationCommentRepository extends JpaRepository<ModerationComment, Long> {

    List<ModerationComment> findByEventIdOrderByCreatedOnDesc(Long eventId);

    List<ModerationComment> findAllByEventIdIn(List<Long> eventIds);
}
