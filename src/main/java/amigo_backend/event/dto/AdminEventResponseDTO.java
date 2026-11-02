package amigo_backend.event.dto;

import amigo_backend.entity.User;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventVerification;
import amigo_backend.event.enums.VerificationDecision;

import java.time.LocalDateTime;

/** Complete event plus its verification record. Admin endpoints only. */
public record AdminEventResponseDTO(
        EventResponseDTO event,
        // Send back as eventVersion with approve/reject/request-information.
        Long eventVersion,
        Verification verification
) {

    public record Verification(
            Long id,
            VerificationDecision decision,
            String officialUrl,
            String ticketUrl,
            String posterUrl,
            String supportingInformation,
            String adminNotes,
            String messageToSubmitter,
            Long submittedByUserId,
            String submittedByEmail,
            Long reviewedByUserId,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt
    ) {
    }

    public static AdminEventResponseDTO from(Event event, EventVerification verification) {

        User reviewer = verification.getReviewedBy();

        return new AdminEventResponseDTO(
                EventResponseDTO.from(event, verification),
                event.getVersion(),
                new Verification(
                        verification.getId(),
                        verification.getDecision(),
                        verification.getEvidenceOfficialUrl(),
                        verification.getEvidenceTicketUrl(),
                        verification.getEvidencePosterUrl(),
                        verification.getEvidenceSupportingInformation(),
                        verification.getAdminNotes(),
                        verification.getMessageToSubmitter(),
                        verification.getSubmittedBy().getId(),
                        verification.getSubmittedBy().getEmail(),
                        reviewer == null ? null : reviewer.getId(),
                        verification.getSubmittedAt(),
                        verification.getReviewedAt()
                )
        );
    }
}