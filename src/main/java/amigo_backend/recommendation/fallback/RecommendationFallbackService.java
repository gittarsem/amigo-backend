package amigo_backend.recommendation.fallback;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ranking used when ML is unavailable or returns nothing usable: eligible
 * users at the event, nearest first (unknown distance last), then most shared
 * interests, then most recently active.
 */
@Service
public class RecommendationFallbackService {

    public static final String MODEL_VERSION = "fallback-distance-interests-v1";

    /** An eligible candidate, with everything needed to rank and display them. */
    public record Candidate(
            Long userId,
            String userName,
            String firstName,
            Double originLatitude,
            Double originLongitude,
            Set<String> interests,
            int activityOrder
    ) {
    }

    public List<Candidate> rank(
            Double viewerLatitude,
            Double viewerLongitude,
            Set<String> viewerInterests,
            List<Candidate> candidates,
            int limit
    ) {
        Comparator<Candidate> byDistance = Comparator.comparing(
                candidate -> distanceKm(viewerLatitude, viewerLongitude, candidate),
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        return candidates.stream()
                .sorted(byDistance
                        .thenComparing(
                                candidate -> sharedInterests(viewerInterests, candidate).size(),
                                Comparator.reverseOrder())
                        .thenComparing(Candidate::activityOrder))
                .limit(limit)
                .toList();
    }

    /** Haversine distance between the two starting points, or null if either is unknown. */
    public static Double distanceKm(Double viewerLatitude, Double viewerLongitude, Candidate candidate) {

        if (viewerLatitude == null || viewerLongitude == null ||
                candidate.originLatitude() == null || candidate.originLongitude() == null) {
            return null;
        }

        double lat1 = Math.toRadians(viewerLatitude);
        double lat2 = Math.toRadians(candidate.originLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(candidate.originLongitude() - viewerLongitude);

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);

        return 2 * 6371.0 * Math.asin(Math.sqrt(a));
    }

    public static List<String> sharedInterests(Set<String> viewerInterests, Candidate candidate) {

        return candidate.interests().stream()
                .filter(viewerInterests::contains)
                .sorted()
                .toList();
    }
}