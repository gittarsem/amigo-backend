package amigo_backend.event.service;

import amigo_backend.common.exception.ApiException;
import amigo_backend.entity.User;
import amigo_backend.event.dto.EventPreferenceDTO;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventSpecificPreference;
import amigo_backend.event.repository.EventSpecificPreferenceRepository;
import amigo_backend.recommendationprofile.enums.RecommendationDimension;
import amigo_backend.recommendationprofile.service.RecommendationProfileService;
import amigo_backend.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static amigo_backend.recommendationprofile.enums.RecommendationDimension.*;

/**
 * Event-specific questions and answers: what the user wants from THIS event.
 * Answers adjust the user's default weights for this event only.
 */
@Service
@RequiredArgsConstructor
public class EventPreferenceService {

    public record Question(String id, String question, boolean required, List<Option> options) {
    }

    public record Option(
            String id,
            String text,
            @JsonIgnore Map<RecommendationDimension, Double> impact
    ) {
        Option(String id, String text) {
            this(id, text, Map.of());
        }
    }

    // Questions with impacts are the ML service's EVENT_SPECIFIC_QUESTIONS
    // (eq1-eq4, same option ids and deltas). The others are the handoff doc's
    // examples; they are stored for matching but do not change weights yet.
    private static final List<Question> QUESTIONS = List.of(
            new Question("buddy_reason", "Why are you looking for a buddy for this event?", false, List.of(
                    new Option("dont_want_to_go_alone", "I don't want to go alone"),
                    new Option("meet_new_people", "To meet new people"),
                    new Option("share_the_experience", "To share the experience with someone who's into it"),
                    new Option("feel_safer", "To feel safer and more comfortable")
            )),
            new Question("eq1", "What matters more for this event?", true, List.of(
                    new Option("same_interest", "Same event/interest compatibility",
                            Map.of(EVENT_FIT, 0.04, INTEREST, 0.03, SOCIAL_FIT, -0.03)),
                    new Option("social_vibe", "Similar social vibe",
                            Map.of(SOCIAL_FIT, 0.05, EVENT_FIT, -0.02))
            )),
            new Question("eq2", "How important is meeting someone nearby?", true, List.of(
                    new Option("not_important", "Not important", Map.of(LOCATION, -0.03, INTEREST, 0.02)),
                    new Option("somewhat_important", "Somewhat important", Map.of(LOCATION, 0.02)),
                    new Option("very_important", "Very important", Map.of(LOCATION, 0.06, INTEREST, -0.03))
            )),
            new Question("eq3", "What do you want from this event?", true, List.of(
                    new Option("just_watch", "Just watch", Map.of(INTENT_FIT, 0.02, SOCIAL_FIT, -0.02)),
                    new Option("watch_and_meet", "Watch + meet people", Map.of(INTENT_FIT, 0.04, SOCIAL_FIT, 0.03)),
                    new Option("watch_and_hangout", "Watch + hangout afterwards",
                            Map.of(INTENT_FIT, 0.06, SOCIAL_FIT, 0.02, PREFS, 0.02))
            )),
            new Question("eq4", "How important is arriving/leaving and staying together?", true, List.of(
                    new Option("not_important", "Not important", Map.of(AVAILABILITY, -0.03, PREFS, -0.02)),
                    new Option("somewhat_important", "Somewhat important", Map.of(AVAILABILITY, 0.02, PREFS, 0.01)),
                    new Option("very_important", "Very important", Map.of(AVAILABILITY, 0.05, PREFS, 0.03))
            )),
            new Question("group_size", "What group size would you like?", false, List.of(
                    new Option("one_buddy", "Just one buddy"),
                    new Option("small_group", "A small group (3-5)"),
                    new Option("large_group", "A bigger group (6+)"),
                    new Option("no_preference", "No preference")
            )),
            new Question("social_energy", "What social energy do you want at this event?", false, List.of(
                    new Option("chill", "Chill and low-key"),
                    new Option("balanced", "Balanced"),
                    new Option("high_energy", "High energy")
            )),
            new Question("new_people_comfort", "How comfortable are you meeting new people?", false, List.of(
                    new Option("low", "It takes me a while"),
                    new Option("medium", "Fairly comfortable"),
                    new Option("high", "Very comfortable")
            )),
            new Question("after_event", "Any plans after the event?", false, List.of(
                    new Option("go_home", "Head home"),
                    new Option("grab_food", "Grab food or drinks"),
                    new Option("keep_partying", "Keep the night going"),
                    new Option("undecided", "Undecided")
            ))
    );

    private final EventSpecificPreferenceRepository eventSpecificPreferenceRepository;
    private final EventService eventService;
    private final RecommendationProfileService recommendationProfileService;
    private final UserRepository userRepository;

    public List<Question> getQuestions() {

        return QUESTIONS;
    }

    @Transactional(readOnly = true)
    public EventPreferenceDTO getMyPreferences(String email, Long eventId) {

        User user = findUserByEmail(email);
        eventService.getRecommendableEvent(eventId);

        EventSpecificPreference preference = eventSpecificPreferenceRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> ApiException.notFound("You have not answered the questions for this event"));

        return toDTO(preference);
    }

    @Transactional
    public EventPreferenceDTO savePreferences(String email, Long eventId, EventPreferenceDTO request) {

        User user = findUserByEmail(email);
        Event event = eventService.getRecommendableEvent(eventId);

        validateAnswers(request.answers());

        if ((request.originLatitude() == null) != (request.originLongitude() == null)) {
            throw ApiException.badRequest("Provide both originLatitude and originLongitude, or neither");
        }

        EventSpecificPreference preference = eventSpecificPreferenceRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElseGet(() -> EventSpecificPreference.builder().user(user).event(event).build());

        preference.getAnswers().clear();
        preference.getAnswers().putAll(request.answers());
        preference.setOriginLatitude(roundCoordinate(request.originLatitude()));
        preference.setOriginLongitude(roundCoordinate(request.originLongitude()));
        // Set explicitly: changing only the answers collection does not trigger @PreUpdate.
        preference.setUpdatedAt(LocalDateTime.now());

        return toDTO(eventSpecificPreferenceRepository.save(preference));
    }

    /** Removes the answers, which also opts the user out of buddy matching for the event. */
    @Transactional
    public void deletePreferences(String email, Long eventId) {

        User user = findUserByEmail(email);

        eventSpecificPreferenceRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .ifPresent(eventSpecificPreferenceRepository::delete);
    }

    /** The user's default weights adjusted by their answers for this event. */
    @Transactional(readOnly = true)
    public Map<RecommendationDimension, Double> getEffectiveWeights(EventSpecificPreference preference) {

        List<Map<RecommendationDimension, Double>> impacts = new ArrayList<>();

        for (Question question : QUESTIONS) {
            String chosen = preference.getAnswers().get(question.id());

            question.options().stream()
                    .filter(option -> option.id().equals(chosen))
                    .findFirst()
                    .ifPresent(option -> impacts.add(option.impact()));
        }

        return recommendationProfileService.applyAdjustments(
                recommendationProfileService.getDefaultWeights(preference.getUser().getId()),
                impacts
        );
    }

    private void validateAnswers(Map<String, String> answers) {

        for (String questionId : answers.keySet()) {
            if (QUESTIONS.stream().noneMatch(question -> question.id().equals(questionId))) {
                throw ApiException.badRequest("Unknown question: " + questionId);
            }
        }

        for (Question question : QUESTIONS) {

            String chosen = answers.get(question.id());

            if (chosen == null) {
                if (question.required()) {
                    throw ApiException.badRequest("Answer is required for question " + question.id());
                }
                continue;
            }

            if (question.options().stream().noneMatch(option -> option.id().equals(chosen))) {
                throw ApiException.badRequest("Invalid option '" + chosen + "' for question " + question.id());
            }
        }
    }

    private EventPreferenceDTO toDTO(EventSpecificPreference preference) {

        return new EventPreferenceDTO(
                new TreeMap<>(preference.getAnswers()),
                preference.getOriginLatitude(),
                preference.getOriginLongitude(),
                recommendationProfileService.toKeyedMap(getEffectiveWeights(preference)),
                preference.getUpdatedAt()
        );
    }

    /** Two decimals (~1 km) is enough for distance matching and avoids storing an exact location. */
    private static Double roundCoordinate(Double value) {

        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}