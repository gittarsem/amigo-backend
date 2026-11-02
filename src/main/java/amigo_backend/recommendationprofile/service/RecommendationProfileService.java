package amigo_backend.recommendationprofile.service;

import amigo_backend.common.exception.ApiException;
import amigo_backend.entity.User;
import amigo_backend.recommendationprofile.dto.RecommendationProfileDTO;
import amigo_backend.recommendationprofile.entity.RecommendationProfile;
import amigo_backend.recommendationprofile.enums.RecommendationDimension;
import amigo_backend.recommendationprofile.repository.RecommendationProfileRepository;
import amigo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default buddy-matching weights per user, plus the weight arithmetic shared
 * with event-specific adjustments. The arithmetic mirrors the ML service
 * (buddy_matching.model.normalize_weights / calculate_event_weights).
 */
@Service
@RequiredArgsConstructor
public class RecommendationProfileService {

    private static final double WEIGHT_FLOOR = 0.01;
    private static final double SCALE = 10_000.0; // round to 4 decimals, as ML does

    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RecommendationProfileDTO getProfile(String email) {

        User user = findUserByEmail(email);

        return recommendationProfileRepository
                .findByUserId(user.getId())
                .map(profile -> new RecommendationProfileDTO(
                        toKeyedMap(normalize(profile.getWeights())),
                        false,
                        profile.getUpdatedAt()
                ))
                .orElseGet(() -> new RecommendationProfileDTO(
                        toKeyedMap(systemDefaults()),
                        true,
                        null
                ));
    }

    @Transactional
    public RecommendationProfileDTO updateProfile(String email, RecommendationProfileDTO request) {

        User user = findUserByEmail(email);
        Map<RecommendationDimension, Double> weights = parseWeights(request.weights());

        RecommendationProfile profile = recommendationProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> RecommendationProfile.builder().user(user).build());

        profile.getWeights().clear();
        profile.getWeights().putAll(weights);
        // Set explicitly: changing only the weights collection does not trigger @PreUpdate.
        profile.setUpdatedAt(LocalDateTime.now());

        RecommendationProfile saved = recommendationProfileRepository.save(profile);

        return new RecommendationProfileDTO(toKeyedMap(weights), false, saved.getUpdatedAt());
    }

    /** Drops the stored weights so the system defaults apply again. */
    @Transactional
    public RecommendationProfileDTO resetProfile(String email) {

        User user = findUserByEmail(email);

        recommendationProfileRepository
                .findByUserId(user.getId())
                .ifPresent(recommendationProfileRepository::delete);

        return new RecommendationProfileDTO(toKeyedMap(systemDefaults()), true, null);
    }

    /** Stored default weights, or the system defaults if the user has none. */
    @Transactional(readOnly = true)
    public Map<RecommendationDimension, Double> getDefaultWeights(Long userId) {

        return recommendationProfileRepository
                .findByUserId(userId)
                .map(profile -> normalize(profile.getWeights()))
                .orElseGet(this::systemDefaults);
    }

    /**
     * Event-specific weights: the defaults plus the deltas of the chosen
     * answers, floored and re-normalised. The input map is not modified.
     */
    public Map<RecommendationDimension, Double> applyAdjustments(
            Map<RecommendationDimension, Double> defaultWeights,
            Collection<Map<RecommendationDimension, Double>> impacts
    ) {
        Map<RecommendationDimension, Double> adjusted = new EnumMap<>(defaultWeights);

        impacts.forEach(impact ->
                impact.forEach((dimension, delta) -> adjusted.merge(dimension, delta, Double::sum))
        );

        return normalize(adjusted);
    }

    public Map<String, Double> toKeyedMap(Map<RecommendationDimension, Double> weights) {

        Map<String, Double> keyed = new LinkedHashMap<>();

        for (RecommendationDimension dimension : RecommendationDimension.values()) {
            keyed.put(dimension.getKey(), weights.get(dimension));
        }

        return keyed;
    }

    private Map<RecommendationDimension, Double> systemDefaults() {

        Map<RecommendationDimension, Double> weights = new EnumMap<>(RecommendationDimension.class);

        for (RecommendationDimension dimension : RecommendationDimension.values()) {
            weights.put(dimension, dimension.getDefaultWeight());
        }

        return weights;
    }

    /**
     * Fills missing dimensions with defaults, floors each at 0.01, scales to
     * sum to 1.0 and rounds to 4 decimals, adding any rounding remainder to
     * the largest weight.
     */
    private Map<RecommendationDimension, Double> normalize(Map<RecommendationDimension, Double> weights) {

        Map<RecommendationDimension, Double> normalized = new EnumMap<>(RecommendationDimension.class);
        double total = 0.0;

        for (RecommendationDimension dimension : RecommendationDimension.values()) {
            double value = Math.max(WEIGHT_FLOOR, weights.getOrDefault(dimension, dimension.getDefaultWeight()));
            normalized.put(dimension, value);
            total += value;
        }

        double sum = 0.0;
        RecommendationDimension largest = RecommendationDimension.INTEREST;

        for (RecommendationDimension dimension : RecommendationDimension.values()) {
            double value = round(normalized.get(dimension) / total);
            normalized.put(dimension, value);
            sum += value;

            if (value > normalized.get(largest)) {
                largest = dimension;
            }
        }

        double remainder = round(1.0 - sum);

        if (remainder != 0.0) {
            normalized.put(largest, round(normalized.get(largest) + remainder));
        }

        return normalized;
    }

    private Map<RecommendationDimension, Double> parseWeights(Map<String, Double> input) {

        Map<RecommendationDimension, Double> weights = new EnumMap<>(RecommendationDimension.class);

        input.forEach((key, value) -> {

            RecommendationDimension dimension = RecommendationDimension.fromKey(key)
                    .orElseThrow(() -> ApiException.badRequest("Unknown weight dimension: " + key));

            if (value == null || !Double.isFinite(value) || value < 0 || value > 100) {
                throw ApiException.badRequest("Weight for " + key + " must be between 0 and 100");
            }

            weights.put(dimension, value);
        });

        if (weights.size() != RecommendationDimension.values().length) {
            throw ApiException.badRequest(
                    "weights must include all dimensions: interest, event_fit, social_fit, prefs, "
                            + "intent_fit, location, availability"
            );
        }

        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();

        if (total <= 0.0) {
            throw ApiException.badRequest("At least one weight must be above 0");
        }

        weights.replaceAll((dimension, value) -> value / total);

        return normalize(weights);
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private static double round(double value) {

        return Math.round(value * SCALE) / SCALE;
    }
}