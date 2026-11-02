package amigo_backend.event.service.impl;

import amigo_backend.common.exception.ApiException;
import amigo_backend.entity.User;
import amigo_backend.enums.Role;
import amigo_backend.event.dto.AdminEventDecisionRequestDTO;
import amigo_backend.event.dto.AdminEventResponseDTO;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventVerification;
import amigo_backend.event.enums.EventStatus;
import amigo_backend.event.enums.VerificationDecision;
import amigo_backend.event.repository.EventRepository;
import amigo_backend.event.repository.EventVerificationRepository;
import amigo_backend.event.service.EventVerificationService;
import amigo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static amigo_backend.event.service.impl.EventServiceImpl.trimToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventVerificationServiceImpl implements EventVerificationService {

    private final EventRepository eventRepository;
    private final EventVerificationRepository eventVerificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public EventVerification submitForReview(Event event, User submitter, String supportingInformation) {

        EventVerification verification = eventVerificationRepository
                .findByEventId(event.getId())
                .orElseGet(() -> EventVerification.builder().event(event).build());

        // The reviewer, review time and admin notes of an earlier round are kept
        // for the audit trail; only the decision starts over.
        verification.setSubmittedBy(submitter);
        verification.setDecision(VerificationDecision.PENDING);
        verification.setSubmittedAt(Event.nowInEventZone());
        verification.setEvidenceOfficialUrl(event.getOfficialUrl());
        verification.setEvidenceTicketUrl(event.getTicketUrl());
        verification.setEvidencePosterUrl(event.getPosterUrl());
        verification.setEvidenceSupportingInformation(trimToNull(supportingInformation));

        return eventVerificationRepository.save(verification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminEventResponseDTO> getPendingEvents(String adminEmail) {

        findAdmin(adminEmail);

        // Oldest submissions first so nothing waits indefinitely.
        return eventRepository
                .findByStatusOrderByUpdatedAtAsc(EventStatus.PENDING_REVIEW)
                .stream()
                .map(event -> AdminEventResponseDTO.from(event, findVerification(event)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEventResponseDTO getEventForReview(String adminEmail, Long eventId) {

        findAdmin(adminEmail);
        Event event = findEvent(eventId);

        return AdminEventResponseDTO.from(event, findVerification(event));
    }

    @Override
    @Transactional
    public AdminEventResponseDTO approve(String adminEmail, Long eventId, AdminEventDecisionRequestDTO request) {

        User admin = findAdmin(adminEmail);
        Event event = findEvent(eventId);

        requireStatus(event, Set.of(EventStatus.PENDING_REVIEW, EventStatus.NEEDS_INFORMATION), "approved");

        if (!event.getEndTime().isAfter(Event.nowInEventZone())) {
            throw ApiException.conflict("An event that has already ended cannot be approved");
        }

        return decide(admin, event, EventStatus.APPROVED, VerificationDecision.APPROVED, request);
    }

    @Override
    @Transactional
    public AdminEventResponseDTO reject(String adminEmail, Long eventId, AdminEventDecisionRequestDTO request) {

        User admin = findAdmin(adminEmail);
        Event event = findEvent(eventId);

        // APPROVED is included so an admin can take down an event found to be fake.
        requireStatus(
                event,
                Set.of(EventStatus.PENDING_REVIEW, EventStatus.NEEDS_INFORMATION, EventStatus.APPROVED),
                "rejected"
        );

        return decide(admin, event, EventStatus.REJECTED, VerificationDecision.REJECTED, request);
    }

    @Override
    @Transactional
    public AdminEventResponseDTO requestInformation(
            String adminEmail,
            Long eventId,
            AdminEventDecisionRequestDTO request
    ) {
        User admin = findAdmin(adminEmail);
        Event event = findEvent(eventId);

        requireStatus(event, Set.of(EventStatus.PENDING_REVIEW), "sent back for more information");

        if (trimToNull(request.messageToSubmitter()) == null) {
            throw ApiException.badRequest("messageToSubmitter is required: tell the submitter what is needed");
        }

        return decide(admin, event, EventStatus.NEEDS_INFORMATION, VerificationDecision.NEEDS_INFORMATION, request);
    }

    private AdminEventResponseDTO decide(
            User admin,
            Event event,
            EventStatus newStatus,
            VerificationDecision decision,
            AdminEventDecisionRequestDTO request
    ) {
        if (!event.getVersion().equals(request.eventVersion())) {
            throw ApiException.conflict("The event changed after you opened it. Reload it and review again");
        }

        EventVerification verification = findVerification(event);
        EventStatus previousStatus = event.getStatus();

        event.setStatus(newStatus);

        verification.setDecision(decision);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(Event.nowInEventZone());
        verification.setMessageToSubmitter(trimToNull(request.messageToSubmitter()));

        if (request.adminNotes() != null) {
            verification.setAdminNotes(trimToNull(request.adminNotes()));
        }

        eventRepository.save(event);
        eventVerificationRepository.save(verification);

        log.info("Event review: eventId={} {} -> {} by adminId={}",
                event.getId(), previousStatus, newStatus, admin.getId());

        return AdminEventResponseDTO.from(event, verification);
    }

    private void requireStatus(Event event, Set<EventStatus> allowed, String action) {

        if (!allowed.contains(event.getStatus())) {
            throw ApiException.conflict("An event with status " + event.getStatus() + " cannot be " + action);
        }
    }

    /**
     * SecurityConfig already requires the ADMIN role from the token; this also
     * checks the stored role so a demoted admin loses access immediately.
     */
    private User findAdmin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Admin access is required");
        }

        return user;
    }

    private Event findEvent(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
    }

    private EventVerification findVerification(Event event) {

        return eventVerificationRepository.findByEventId(event.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Event " + event.getId() + " has no verification record"
                ));
    }
}