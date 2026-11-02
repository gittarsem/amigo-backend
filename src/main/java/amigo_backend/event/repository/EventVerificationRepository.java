package amigo_backend.event.repository;

import amigo_backend.event.entity.EventVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventVerificationRepository
        extends JpaRepository<EventVerification, Long> {

    Optional<EventVerification> findByEventId(Long eventId);
}