package amigo_backend.event.repository;

import amigo_backend.event.entity.EventSpecificPreference;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSpecificPreferenceRepository
        extends JpaRepository<EventSpecificPreference, Long> {

    Optional<EventSpecificPreference> findByUserIdAndEventId(Long userId, Long eventId);

    /** Other users opted in to buddy matching for the event, most recently active first. */
    List<EventSpecificPreference> findByEventIdAndUserIdNotOrderByUpdatedAtDesc(
            Long eventId,
            Long userId,
            Limit limit
    );
}