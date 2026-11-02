package amigo_backend.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BuddyRecommendationRequestDTO(

        @NotNull(message = "eventId is required")
        Long eventId,

        // Defaults to 10 when omitted.
        @Min(value = 1, message = "limit must be between 1 and 50")
        @Max(value = 50, message = "limit must be between 1 and 50")
        Integer limit

) {
}