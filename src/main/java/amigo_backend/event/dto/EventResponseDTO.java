package amigo_backend.event.dto;

import amigo_backend.enums.InterestCategory;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventVerification;
import amigo_backend.event.enums.EventStatus;
import amigo_backend.event.enums.VerificationDecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EventResponseDTO(
        Long id,
        String title,
        String description,
        InterestCategory category,
        List<String> tags,
        String venueName,
        String address,
        Double latitude,
        Double longitude,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal budget,
        String organizerName,
        String officialUrl,
        String ticketUrl,
        String posterUrl,
        EventStatus status,
        Long createdByUserId,
        String createdByUserName,
        // Only for the submitter and admins; null for everyone else.
        Review review,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** Review outcome as the submitter sees it. Admin notes are deliberately excluded. */
    public record Review(
            VerificationDecision decision,
            String messageToSubmitter,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt
    ) {
    }

    /** @param verification pass null to omit the review (non-owner viewers) */
    public static EventResponseDTO from(Event event, EventVerification verification) {

        return new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                List.copyOf(event.getTags()),
                event.getVenueName(),
                event.getAddress(),
                event.getLatitude(),
                event.getLongitude(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBudget(),
                event.getOrganizerName(),
                event.getOfficialUrl(),
                event.getTicketUrl(),
                event.getPosterUrl(),
                event.getStatus(),
                event.getCreatedBy().getId(),
                event.getCreatedBy().getUserName(),
                verification == null
                        ? null
                        : new Review(
                                verification.getDecision(),
                                verification.getMessageToSubmitter(),
                                verification.getSubmittedAt(),
                                verification.getReviewedAt()
                        ),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}