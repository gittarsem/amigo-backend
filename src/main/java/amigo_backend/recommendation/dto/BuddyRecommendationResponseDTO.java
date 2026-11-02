package amigo_backend.recommendation.dto;

import java.util.List;
import java.util.Map;

/**
 * Ranked buddies for one event. Clients should send requestId back with later
 * actions (buddy request sent, accepted, RSVP) so they can be tied to this
 * recommendation.
 */
public record BuddyRecommendationResponseDTO(
        String requestId,
        Long eventId,
        String modelVersion,
        boolean fallbackUsed,
        Map<String, Double> weightsUsed,
        List<Buddy> buddies
) {

    public record Buddy(
            int rank,
            Long userId,
            String userName,
            String firstName,
            // ML score in [0, 1]; null when the fallback ranking was used.
            Double score,
            // Distance bucket such as "2-5 km"; null when either location is unknown.
            String distance,
            List<String> sharedInterests
    ) {
    }
}