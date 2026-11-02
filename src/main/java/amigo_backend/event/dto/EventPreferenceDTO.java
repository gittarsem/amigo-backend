package amigo_backend.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request: answers (question id -> option id) and an optional starting point.
 * Response: the same, plus the effective weights for this event (defaults
 * adjusted by the answers) and when they were saved.
 */
public record EventPreferenceDTO(

        @NotEmpty(message = "answers are required")
        @Size(max = 20, message = "Too many answers")
        Map<String, String> answers,

        @DecimalMin(value = "-90.0", message = "originLatitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "originLatitude must be between -90 and 90")
        Double originLatitude,

        @DecimalMin(value = "-180.0", message = "originLongitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "originLongitude must be between -180 and 180")
        Double originLongitude,

        Map<String, Double> effectiveWeights,

        LocalDateTime updatedAt

) {
}