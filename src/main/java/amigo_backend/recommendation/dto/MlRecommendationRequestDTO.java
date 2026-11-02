package amigo_backend.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Body of the ML service's POST /v1/match/recommend. Field names follow the
 * ML contract (mixed camelCase and snake_case).
 */
public record MlRecommendationRequestDTO(
        Long userId,
        Long eventId,
        int limit,
        @JsonProperty("viewer_taste") Taste viewerTaste,
        // Taste profiles of the candidates Spring found eligible.
        @JsonProperty("candidate_tastes") List<Taste> candidateTastes,
        @JsonProperty("event_profile") EventProfile eventProfile,
        // Effective weights (defaults + event adjustments) computed by Spring.
        @JsonProperty("custom_weights") Map<String, Double> customWeights,
        @JsonProperty("use_event_adjustments") boolean useEventAdjustments,
        @JsonProperty("include_details") boolean includeDetails
) {

    public record Taste(
            Long userId,
            Map<String, Double> dimensions,
            Map<String, Double> confidence,
            Map<String, Double> tags
    ) {
    }

    public record EventProfile(
            Long eventId,
            String category,
            List<String> tags,
            String title,
            String description,
            @JsonProperty("venue_lat") Double venueLat,
            @JsonProperty("venue_lng") Double venueLng,
            // 1 = low, 2 = medium, 3 = high cost.
            @JsonProperty("price_band") int priceBand
    ) {
    }
}