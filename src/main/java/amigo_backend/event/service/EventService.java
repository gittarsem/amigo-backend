package amigo_backend.event.service;

import amigo_backend.event.dto.EventRequestDTO;
import amigo_backend.event.dto.EventResponseDTO;
import amigo_backend.event.entity.Event;

import java.util.List;

public interface EventService {

    EventResponseDTO createEvent(String email, EventRequestDTO request);

    EventResponseDTO getEvent(String email, Long eventId);

    EventResponseDTO updateEvent(String email, Long eventId, EventRequestDTO request);

    EventResponseDTO cancelEvent(String email, Long eventId);

    /** Approved events that have not ended, soonest first. */
    List<EventResponseDTO> getApprovedEvents();

    List<EventResponseDTO> getMyEvents(String email);

    /**
     * An event buddy matching may use: exists, APPROVED and not yet ended.
     * For other services, inside their transaction.
     */
    Event getRecommendableEvent(Long eventId);
}