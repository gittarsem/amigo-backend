package amigo_backend.event.repository;

import amigo_backend.event.entity.Event;
import amigo_backend.event.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusAndEndTimeAfterOrderByStartTimeAsc(
            EventStatus status,
            LocalDateTime now
    );

    List<Event> findByStatusOrderByUpdatedAtAsc(EventStatus status);

    List<Event> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
}