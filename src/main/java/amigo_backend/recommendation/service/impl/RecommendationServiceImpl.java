package amigo_backend.recommendation.service.impl;

import amigo_backend.common.exception.ApiException;
import amigo_backend.entity.TasteProfile;
import amigo_backend.entity.User;
import amigo_backend.enums.InterestTag;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventSpecificPreference;
import amigo_backend.event.repository.EventRepository;
import amigo_backend.event.repository.EventSpecificPreferenceRepository;
import amigo_backend.event.service.EventPreferenceService;
import amigo_backend.event.service.EventService;
import amigo_backend.recommendation.dto.BuddyRecommendationRequestDTO;
import amigo_backend.recommendation.dto.BuddyRecommendationResponseDTO;
import amigo_backend.recommendation.dto.MlRecommendationRequestDTO;
import amigo_backend.recommendation.entity.RecommendationLog;
import amigo_backend.recommendation.fallback.RecommendationFallbackService;
import amigo_backend.recommendation.fallback.RecommendationFallbackService.Candidate;
import amigo_backend.recommendation.repository.RecommendationLogRepository;
import amigo_backend.recommendation.service.MlRecommendationClient;
import amigo_backend.recommendation.service.MlRecommendationClient.MlResponse;
import amigo_backend.recommendation.service.MlRecommendationClient.MlServiceException;
import amigo_backend.recommendation.service.RecommendationService;
import amigo_backend.recommendationprofile.service.RecommendationProfileService;
import amigo_backend.repository.TasteProfileRepository;
import amigo_backend.repository.UserInterestRepository;
import amigo_backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Buddy recommendation orchestration. Spring owns eligibility; ML only ranks
 * the eligible candidates; the fallback ranks them when ML cannot.
 *
 * Data is loaded and the log saved in short transactions, and the ML call
 * happens between them, so no database connection is held during the call.
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_LIMIT = 10;

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final EventPreferenceService eventPreferenceService;
    private final EventSpecificPreferenceRepository eventSpecificPreferenceRepository;
    private final RecommendationProfileService recommendationProfileService;
    private final TasteProfileRepository tasteProfileRepository;
    private final UserInterestRepository userInterestRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final MlRecommendationClient mlRecommendationClient;
    private final RecommendationFallbackService recommendationFallbackService;
    private final TransactionTemplate readTransaction;
    private final TransactionTemplate writeTransaction;
    private final int maxCandidates;

    public RecommendationServiceImpl(
            UserRepository userRepository,
            EventRepository eventRepository,
            EventService eventService,
            EventPreferenceService eventPreferenceService,
            EventSpecificPreferenceRepository eventSpecificPreferenceRepository,
            RecommendationProfileService recommendationProfileService,
            TasteProfileRepository tasteProfileRepository,
            UserInterestRepository userInterestRepository,
            RecommendationLogRepository recommendationLogRepository,
            MlRecommendationClient mlRecommendationClient,
            RecommendationFallbackService recommendationFallbackService,
            PlatformTransactionManager transactionManager,
            @Value("${ml.service.max-candidates:200}") int maxCandidates
    ) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.eventPreferenceService = eventPreferenceService;
        this.eventSpecificPreferenceRepository = eventSpecificPreferenceRepository;
        this.recommendationProfileService = recommendationProfileService;
        this.tasteProfileRepository = tasteProfileRepository;
        this.userInterestRepository = userInterestRepository;
        this.recommendationLogRepository = recommendationLogRepository;
        this.mlRecommendationClient = mlRecommendationClient;
        this.recommendationFallbackService = recommendationFallbackService;
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.maxCandidates = maxCandidates;
    }

    @Override
    public BuddyRecommendationResponseDTO recommendBuddies(String email, BuddyRecommendationRequestDTO request) {

        long startedAt = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();

        Context context = readTransaction.execute(status -> loadContext(email, request.eventId(), limit));

        Map<Long, Candidate> eligible = context.candidates().stream()
                .collect(Collectors.toMap(Candidate::userId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<Candidate> ranked;
        Map<Long, Double> scores = Map.of();
        String modelVersion = null;
        String errorCode = null;
        boolean fallbackUsed = false;

        if (!eligible.isEmpty()) {
            try {
                MlResponse response = mlRecommendationClient.recommend(context.mlRequest(), requestId);

                // ML must only rank the candidates Spring supplied: drop anything else.
                scores = new LinkedHashMap<>();
                for (MlRecommendationClient.Recommendation recommendation : response.recommendations()) {
                    if (recommendation.candidateUserId() != null && eligible.containsKey(recommendation.candidateUserId())) {
                        scores.putIfAbsent(recommendation.candidateUserId(), recommendation.score());
                    }
                }

                int dropped = response.recommendations().size() - scores.size();
                if (dropped > 0) {
                    log.warn("ML returned {} ineligible or duplicate candidates, dropped; requestId={}", dropped, requestId);
                }

                if (scores.isEmpty()) {
                    throw new MlServiceException("ML_NO_ELIGIBLE_RESULTS", "ML returned no eligible candidates");
                }

                ranked = new ArrayList<>(scores.keySet().stream().limit(limit).map(eligible::get).toList());
                modelVersion = response.modelVersion() != null ? response.modelVersion() : "ml-unreported";

                // ML only orders; it must not hide eligible users. Fill the
                // remaining slots with the ones it left out, in fallback order.
                Map<Long, Double> mlScores = scores;
                List<Candidate> notRankedByMl = context.candidates().stream()
                        .filter(candidate -> !mlScores.containsKey(candidate.userId()))
                        .toList();

                if (ranked.size() < limit && !notRankedByMl.isEmpty()) {
                    ranked.addAll(recommendationFallbackService.rank(
                            context.viewerLatitude(),
                            context.viewerLongitude(),
                            context.viewerInterests(),
                            notRankedByMl,
                            limit - ranked.size()
                    ));
                    errorCode = "ML_PARTIAL";
                }

            } catch (MlServiceException exception) {
                if (mlRecommendationClient.isEnabled()) {
                    log.warn("Buddy recommendation fell back to non-ML ranking: code={} reason={} requestId={}",
                            exception.getErrorCode(), exception.getMessage(), requestId);
                }
                errorCode = exception.getErrorCode();
                fallbackUsed = true;
                scores = Map.of();
                modelVersion = RecommendationFallbackService.MODEL_VERSION;
                ranked = recommendationFallbackService.rank(
                        context.viewerLatitude(),
                        context.viewerLongitude(),
                        context.viewerInterests(),
                        context.candidates(),
                        limit
                );
            }
        } else {
            ranked = List.of();
        }

        List<BuddyRecommendationResponseDTO.Buddy> buddies = new ArrayList<>();

        for (Candidate candidate : ranked) {
            Double distanceKm = RecommendationFallbackService.distanceKm(
                    context.viewerLatitude(), context.viewerLongitude(), candidate);

            buddies.add(new BuddyRecommendationResponseDTO.Buddy(
                    buddies.size() + 1,
                    candidate.userId(),
                    candidate.userName(),
                    candidate.firstName(),
                    scores.get(candidate.userId()),
                    distanceKm == null ? null : distanceBucket(distanceKm),
                    RecommendationFallbackService.sharedInterests(context.viewerInterests(), candidate)
            ));
        }

        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        saveLog(requestId, context, modelVersion, fallbackUsed, latencyMs, errorCode, buddies);

        log.info("Buddy recommendation requestId={} userId={} eventId={} modelVersion={} fallbackUsed={} "
                        + "latencyMs={} candidateCount={} returned={} errorCode={}",
                requestId, context.viewerId(), context.eventId(), modelVersion, fallbackUsed,
                latencyMs, eligible.size(), buddies.size(), errorCode);

        return new BuddyRecommendationResponseDTO(
                requestId,
                context.eventId(),
                modelVersion,
                fallbackUsed,
                context.mlRequest().customWeights(),
                buddies
        );
    }

    private Context loadContext(String email, Long eventId, int limit) {

        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Event event = eventService.getRecommendableEvent(eventId);

        EventSpecificPreference viewerPreference = eventSpecificPreferenceRepository
                .findByUserIdAndEventId(viewer.getId(), eventId)
                .orElseThrow(() -> ApiException.badRequest(
                        "Answer the questions for this event before looking for buddies"
                ));

        Map<String, Double> weights = recommendationProfileService.toKeyedMap(
                eventPreferenceService.getEffectiveWeights(viewerPreference)
        );

        // Eligibility: other users who opted in to buddy matching for this
        // approved, not-yet-ended event. Further business rules (blocked users,
        // verification or age requirements) belong here, never in ML.
        List<EventSpecificPreference> candidatePreferences = eventSpecificPreferenceRepository
                .findByEventIdAndUserIdNotOrderByUpdatedAtDesc(eventId, viewer.getId(), Limit.of(maxCandidates));

        List<Long> userIds = new ArrayList<>();
        userIds.add(viewer.getId());
        candidatePreferences.forEach(preference -> userIds.add(preference.getUser().getId()));

        Map<Long, TasteProfile> tastes = tasteProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        Map<Long, Set<String>> interests = userInterestRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        interest -> interest.getUser().getId(),
                        Collectors.mapping(interest -> interest.getTag().getValue(), Collectors.toSet())
                ));

        List<Candidate> candidates = new ArrayList<>();

        for (EventSpecificPreference preference : candidatePreferences) {
            User candidate = preference.getUser();
            candidates.add(new Candidate(
                    candidate.getId(),
                    candidate.getUserName(),
                    candidate.getFirstName(),
                    preference.getOriginLatitude(),
                    preference.getOriginLongitude(),
                    interests.getOrDefault(candidate.getId(), Set.of()),
                    candidates.size()
            ));
        }

        MlRecommendationRequestDTO mlRequest = new MlRecommendationRequestDTO(
                viewer.getId(),
                event.getId(),
                limit,
                toMlTaste(viewer.getId(), tastes.get(viewer.getId())),
                candidates.stream().map(candidate -> toMlTaste(candidate.userId(), tastes.get(candidate.userId()))).toList(),
                toMlEventProfile(event),
                weights,
                false,
                true
        );

        return new Context(
                viewer.getId(),
                event.getId(),
                viewerPreference.getOriginLatitude(),
                viewerPreference.getOriginLongitude(),
                interests.getOrDefault(viewer.getId(), Set.of()),
                candidates,
                mlRequest
        );
    }

    private void saveLog(
            String requestId,
            Context context,
            String modelVersion,
            boolean fallbackUsed,
            long latencyMs,
            String errorCode,
            List<BuddyRecommendationResponseDTO.Buddy> buddies
    ) {
        try {
            writeTransaction.executeWithoutResult(status -> {
                RecommendationLog entry = RecommendationLog.builder()
                        .requestId(requestId)
                        .user(userRepository.getReferenceById(context.viewerId()))
                        .event(eventRepository.getReferenceById(context.eventId()))
                        .modelVersion(modelVersion)
                        .fallbackUsed(fallbackUsed)
                        .latencyMs(latencyMs)
                        .candidateCount(context.candidates().size())
                        .errorCode(errorCode)
                        .build();

                buddies.forEach(buddy -> entry.getImpressions().add(
                        new RecommendationLog.Impression(buddy.rank(), buddy.userId(), buddy.score())
                ));

                recommendationLogRepository.save(entry);
            });
        } catch (RuntimeException exception) {
            // Tracking must not take recommendations down with it.
            log.error("Could not save recommendation log requestId={}", requestId, exception);
        }
    }

    /** Taste in the ML format; an empty taste means "unknown" to ML, not neutral. */
    private MlRecommendationRequestDTO.Taste toMlTaste(Long userId, TasteProfile profile) {

        if (profile == null) {
            return new MlRecommendationRequestDTO.Taste(userId, Map.of(), Map.of(), Map.of());
        }

        return new MlRecommendationRequestDTO.Taste(
                userId,
                profile.getDimensionScores().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)),
                profile.getDimensionConfidence().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)),
                new HashMap<>(profile.getTagScores())
        );
    }

    private MlRecommendationRequestDTO.EventProfile toMlEventProfile(Event event) {

        return new MlRecommendationRequestDTO.EventProfile(
                event.getId(),
                event.getCategory().name().toLowerCase(Locale.ROOT),
                event.getTags().stream().map(RecommendationServiceImpl::toMlTag).toList(),
                event.getTitle(),
                event.getDescription(),
                event.getLatitude(),
                event.getLongitude(),
                priceBand(event.getBudget())
        );
    }

    /** InterestTag names map to the same values used in Taste Profile tags ("MUSIC_FESTIVALS" -> "music festivals"). */
    private static String toMlTag(String tag) {

        return Arrays.stream(InterestTag.values())
                .filter(interestTag -> interestTag.name().equals(tag))
                .map(InterestTag::getValue)
                .findFirst()
                .orElse(tag.toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private static int priceBand(BigDecimal budget) {

        if (budget == null) {
            return 2;
        }
        if (budget.compareTo(BigDecimal.valueOf(500)) <= 0) {
            return 1;
        }
        return budget.compareTo(BigDecimal.valueOf(2000)) <= 0 ? 2 : 3;
    }

    /** Same buckets as the ML service, so exact locations are never exposed. */
    private static String distanceBucket(double km) {

        if (km < 2) {
            return "< 2 km";
        }
        if (km < 5) {
            return "2-5 km";
        }
        return km < 10 ? "5-10 km" : "10+ km";
    }

    private record Context(
            Long viewerId,
            Long eventId,
            Double viewerLatitude,
            Double viewerLongitude,
            Set<String> viewerInterests,
            List<Candidate> candidates,
            MlRecommendationRequestDTO mlRequest
    ) {
    }
}