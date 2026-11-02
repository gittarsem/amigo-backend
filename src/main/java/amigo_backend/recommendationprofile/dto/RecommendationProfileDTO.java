package amigo_backend.recommendationprofile.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request: {"weights": {"interest": 30, "event_fit": 25, ...}} with all seven
 * dimensions; any scale is accepted and normalised to sum to 1.0.
 * Response: the normalised weights, whether they are the system defaults, and
 * when they were last changed.
 */
public record RecommendationProfileDTO(

        @NotEmpty(message = "weights are required")
        Map<String, Double> weights,

        Boolean systemDefault,

        LocalDateTime updatedAt

) {
}