package amigo_backend.recommendationprofile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * The seven buddy-matching dimensions and their system default weights.
 * Keys and defaults match the ML service (buddy_matching.model.KEYS /
 * DEFAULT_WEIGHTS); keep them in sync.
 */
@Getter
@RequiredArgsConstructor
public enum RecommendationDimension {

    INTEREST("interest", 0.30),
    EVENT_FIT("event_fit", 0.25),
    SOCIAL_FIT("social_fit", 0.15),
    PREFS("prefs", 0.10),
    INTENT_FIT("intent_fit", 0.10),
    LOCATION("location", 0.05),
    AVAILABILITY("availability", 0.05);

    private final String key;
    private final double defaultWeight;

    public static Optional<RecommendationDimension> fromKey(String key) {

        return Arrays.stream(values())
                .filter(dimension -> dimension.key.equals(key))
                .findFirst();
    }
}