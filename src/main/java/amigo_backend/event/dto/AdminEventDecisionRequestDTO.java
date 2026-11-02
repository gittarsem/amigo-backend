package amigo_backend.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminEventDecisionRequestDTO(

        // The eventVersion the admin reviewed (from the admin GET). If the owner
        // edited the event since, the decision is refused so unseen changes are
        // never approved.
        @NotNull(message = "eventVersion is required")
        Long eventVersion,

        // Internal notes, visible to admins only.
        @Size(max = 2000, message = "Admin notes must be at most 2000 characters")
        String adminNotes,

        // Shown to the submitter: the rejection reason or the information needed.
        @Size(max = 1000, message = "Message to submitter must be at most 1000 characters")
        String messageToSubmitter

) {
}