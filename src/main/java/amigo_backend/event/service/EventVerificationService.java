package amigo_backend.event.service;

import amigo_backend.entity.User;
import amigo_backend.event.dto.AdminEventDecisionRequestDTO;
import amigo_backend.event.dto.AdminEventResponseDTO;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventVerification;

import java.util.List;

public interface EventVerificationService {

    /** Opens, or re-opens after an edit, the review record for an event. */
    EventVerification submitForReview(Event event, User submitter, String supportingInformation);

    List<AdminEventResponseDTO> getPendingEvents(String adminEmail);

    AdminEventResponseDTO getEventForReview(String adminEmail, Long eventId);

    AdminEventResponseDTO approve(String adminEmail, Long eventId, AdminEventDecisionRequestDTO request);

    AdminEventResponseDTO reject(String adminEmail, Long eventId, AdminEventDecisionRequestDTO request);

    AdminEventResponseDTO requestInformation(String adminEmail, Long eventId, AdminEventDecisionRequestDTO request);
}