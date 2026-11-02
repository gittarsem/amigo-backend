package amigo_backend;

import amigo_backend.enums.Role;
import amigo_backend.event.entity.Event;
import amigo_backend.recommendation.entity.RecommendationLog;
import amigo_backend.recommendation.repository.RecommendationLogRepository;
import amigo_backend.recommendation.service.MlRecommendationClient;
import amigo_backend.recommendation.service.MlRecommendationClient.MlResponse;
import amigo_backend.recommendation.service.MlRecommendationClient.MlServiceException;
import amigo_backend.recommendation.service.MlRecommendationClient.Recommendation;
import amigo_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Event lifecycle (listing -> admin review -> discovery) and buddy
 * recommendation with ML success and failure. Each test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventRecommendationFlowTests {

    private static final AtomicInteger USER_COUNTER = new AtomicInteger();

    private static final String EVENT_QUESTION_ANSWERS = """
            "answers": {"eq1": "same_interest", "eq2": "very_important", "eq3": "watch_and_meet", "eq4": "somewhat_important"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationLogRepository recommendationLogRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private MlRecommendationClient mlRecommendationClient;

    @Test
    void eventGoesThroughReviewBeforeItIsDiscoverable() throws Exception {

        String owner = signup();
        String other = signup();
        String admin = signupAdmin();

        long eventId = createEvent(owner);

        // Pending: hidden from others and from discovery, visible to the owner.
        get("/api/events/" + eventId, other).andExpect(status().isNotFound());
        get("/api/events", other).andExpect(jsonPath("$[*].id", not(hasItem((int) eventId))));
        get("/api/users/me/events", owner)
                .andExpect(jsonPath("$[0].id").value(eventId))
                .andExpect(jsonPath("$[0].review.decision").value("PENDING"));

        // Only admins may review.
        get("/api/admin/events/pending", other).andExpect(status().isForbidden());
        post("/api/admin/events/" + eventId + "/approve", other, "{\"eventVersion\": 0}")
                .andExpect(status().isForbidden());

        // Request information: the message is required and is shown to the owner; admin notes are not.
        decide(admin, eventId, "request-information", reviewedVersion(admin, eventId), "")
                .andExpect(status().isBadRequest());
        decide(admin, eventId, "request-information", reviewedVersion(admin, eventId),
                ", \"messageToSubmitter\": \"Please add the ticket link\", \"adminNotes\": \"internal-note\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.status").value("NEEDS_INFORMATION"))
                .andExpect(jsonPath("$.verification.adminNotes").value("internal-note"));
        get("/api/events/" + eventId, owner)
                .andExpect(jsonPath("$.status").value("NEEDS_INFORMATION"))
                .andExpect(jsonPath("$.review.messageToSubmitter").value("Please add the ticket link"))
                .andExpect(jsonPath("$..adminNotes").isEmpty());

        // An admin decision on a version the owner has since edited is refused.
        long versionBeforeEdit = reviewedVersion(admin, eventId);

        // Editing resubmits for review.
        put("/api/events/" + eventId, owner, eventJson("EDM Night v2", "https://example.com/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.review.decision").value("PENDING"));

        decide(admin, eventId, "approve", versionBeforeEdit, "").andExpect(status().isConflict());

        // Approved: discoverable by everyone.
        decide(admin, eventId, "approve", reviewedVersion(admin, eventId), "")
                .andExpect(jsonPath("$.event.status").value("APPROVED"))
                .andExpect(jsonPath("$.verification.ticketUrl").value("https://example.com/tickets"));
        get("/api/events/" + eventId, other)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").doesNotExist());
        get("/api/events", other).andExpect(jsonPath("$[*].id", hasItem((int) eventId)));

        // Rejected (taken down): hidden again.
        decide(admin, eventId, "reject", reviewedVersion(admin, eventId), ", \"messageToSubmitter\": \"Not genuine\"")
                .andExpect(jsonPath("$.event.status").value("REJECTED"));
        get("/api/events/" + eventId, other).andExpect(status().isNotFound());
        put("/api/events/" + eventId, owner, eventJson("Again", null)).andExpect(status().isConflict());
    }

    @Test
    void eventRequestsAreValidatedOnTheServer() throws Exception {

        String owner = signup();

        post("/api/events", owner, eventJson("Past", null, -1, 3, "https://example.com"))
                .andExpect(status().isBadRequest());
        post("/api/events", owner, eventJson("Backwards", null, 5, -2, "https://example.com"))
                .andExpect(status().isBadRequest());
        post("/api/events", owner, eventJson("Bad URL", null, 5, 3, "ftp://example.com"))
                .andExpect(status().isBadRequest());
        post("/api/events", owner, "{\"title\": \"x\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventPreferencesNeedAnApprovedEventAndRequiredAnswers() throws Exception {

        String owner = signup();
        String admin = signupAdmin();
        long eventId = createEvent(owner);

        put("/api/events/" + eventId + "/preferences", owner, "{" + EVENT_QUESTION_ANSWERS + "}")
                .andExpect(status().isNotFound());

        approve(admin, eventId);

        put("/api/events/" + eventId + "/preferences", owner, "{\"answers\": {\"eq1\": \"same_interest\"}}")
                .andExpect(status().isBadRequest());
        put("/api/events/" + eventId + "/preferences", owner,
                "{" + EVENT_QUESTION_ANSWERS + ", \"originLatitude\": 28.6139, \"originLongitude\": 77.2090}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originLatitude").value(28.61))
                // eq2=very_important raises location above its 0.05 default.
                .andExpect(jsonPath("$.effectiveWeights.location", greaterThan(0.05)));

        // Defaults are not overwritten by event answers.
        get("/api/recommendation-profile", owner)
                .andExpect(jsonPath("$.systemDefault").value(true))
                .andExpect(jsonPath("$.weights.location").value(0.05));
    }

    @Test
    void buddiesFallBackToDistanceRankingWhenMlFails() throws Exception {

        when(mlRecommendationClient.recommend(any(), anyString()))
                .thenThrow(new MlServiceException("ML_HTTP_503", "unavailable"));

        Scenario scenario = scenarioWithCandidates();

        String body = post("/api/recommendations/buddies", scenario.viewer, "{\"eventId\": " + scenario.eventId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackUsed").value(true))
                .andExpect(jsonPath("$.buddies[0].userId").value(scenario.nearUserId))
                .andExpect(jsonPath("$.buddies[0].distance").value("< 2 km"))
                .andExpect(jsonPath("$.buddies[1].userId").value(scenario.farUserId))
                .andExpect(jsonPath("$.buddies[*].userId", not(hasItem((int) scenario.viewerId))))
                .andReturn().getResponse().getContentAsString();

        RecommendationLog entry = recommendationLogRepository
                .findByRequestId(JsonPath.read(body, "$.requestId"))
                .orElseThrow();
        assertThat(entry.isFallbackUsed()).isTrue();
        assertThat(entry.getErrorCode()).isEqualTo("ML_HTTP_503");
        assertThat(entry.getCandidateCount()).isEqualTo(2);
        assertThat(entry.getImpressions()).hasSize(2);
    }

    @Test
    void buddiesUseMlOrderWithoutLettingMlDropOrAddCandidates() throws Exception {

        Scenario scenario = scenarioWithCandidates();

        // ML returns a user Spring did not find eligible, and leaves out an eligible one.
        when(mlRecommendationClient.recommend(any(), anyString()))
                .thenReturn(new MlResponse(List.of(
                        new Recommendation(987_654_321L, 0.99),
                        new Recommendation(scenario.farUserId, 0.91)
                ), null, null));

        String body = post("/api/recommendations/buddies", scenario.viewer, "{\"eventId\": " + scenario.eventId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackUsed").value(false))
                .andExpect(jsonPath("$.buddies.length()").value(2))
                .andExpect(jsonPath("$.buddies[0].userId").value(scenario.farUserId))
                .andExpect(jsonPath("$.buddies[0].score").value(0.91))
                .andExpect(jsonPath("$.buddies[1].userId").value(scenario.nearUserId))
                .andExpect(jsonPath("$.buddies[1].score").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(recommendationLogRepository.findByRequestId(JsonPath.read(body, "$.requestId"))
                .orElseThrow()
                .getErrorCode()).isEqualTo("ML_PARTIAL");
    }

    @Test
    void buddiesNeedTheViewersEventAnswers() throws Exception {

        String owner = signup();
        String admin = signupAdmin();
        long eventId = createEvent(owner);
        approve(admin, eventId);

        post("/api/recommendations/buddies", owner, "{\"eventId\": " + eventId + "}")
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    private record Scenario(String viewer, long viewerId, long eventId, long nearUserId, long farUserId) {
    }

    /** Approved event; viewer and two candidates opted in; one candidate ~1 km away, one ~20 km. */
    private Scenario scenarioWithCandidates() throws Exception {

        String viewer = signup();
        String near = signup();
        String far = signup();
        String admin = signupAdmin();

        long eventId = createEvent(viewer);
        approve(admin, eventId);

        savePreferences(viewer, eventId, 28.61, 77.20);
        savePreferences(far, eventId, 28.45, 77.03);
        savePreferences(near, eventId, 28.62, 77.21);

        return new Scenario(viewer, userId(viewer), eventId, userId(near), userId(far));
    }

    private void savePreferences(String token, long eventId, double latitude, double longitude) throws Exception {

        put("/api/events/" + eventId + "/preferences", token,
                "{" + EVENT_QUESTION_ANSWERS + ", \"originLatitude\": " + latitude
                        + ", \"originLongitude\": " + longitude + "}")
                .andExpect(status().isOk());
    }

    private long createEvent(String token) throws Exception {

        String body = post("/api/events", token, eventJson("EDM Night", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.tags", contains("EDM", "LIVE_EVENT", "MUSIC_FESTIVALS")))
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void approve(String adminToken, long eventId) throws Exception {

        decide(adminToken, eventId, "approve", reviewedVersion(adminToken, eventId), "")
                .andExpect(status().isOk());
    }

    /** The eventVersion an admin sees when opening the event for review. */
    private long reviewedVersion(String adminToken, long eventId) throws Exception {

        // All requests share the test transaction; flush so @Version moves as it would between real requests.
        entityManager.flush();

        return ((Number) JsonPath.read(
                get("/api/admin/events/" + eventId, adminToken).andReturn().getResponse().getContentAsString(),
                "$.eventVersion"
        )).longValue();
    }

    private ResultActions decide(String adminToken, long eventId, String action, long version, String extraJson)
            throws Exception {

        entityManager.flush();

        return post("/api/admin/events/" + eventId + "/" + action, adminToken,
                "{\"eventVersion\": " + version + extraJson + "}");
    }

    private String eventJson(String title, String ticketUrl) {

        return eventJson(title, ticketUrl, 10, 4, "https://example.com/event");
    }

    private String eventJson(String title, String ticketUrl, int startInDays, int durationHours, String officialUrl) {

        var start = Event.nowInEventZone().plusDays(startInDays).withNano(0);

        return """
                {
                  "title": "%s",
                  "description": "Live EDM night.",
                  "category": "MUSIC",
                  "tags": ["edm", "LIVE_EVENT", "music festivals"],
                  "venueName": "Example Arena",
                  "address": "Example address",
                  "latitude": 28.61,
                  "longitude": 77.20,
                  "startTime": "%s",
                  "endTime": "%s",
                  "budget": 1500,
                  "organizerName": "Example Organizer",
                  "officialUrl": "%s",
                  "ticketUrl": %s,
                  "supportingInformation": "Listed on the venue website"
                }
                """.formatted(
                title,
                start,
                start.plusHours(durationHours),
                officialUrl,
                ticketUrl == null ? "null" : "\"" + ticketUrl + "\""
        );
    }

    private String signup() throws Exception {

        int n = USER_COUNTER.incrementAndGet();
        String unique = System.nanoTime() + "-" + n;
        String email = "flowtest-" + unique + "@example.com";

        mockMvc.perform(postJson("/api/auth/signup", """
                        {"firstName": "Test", "userName": "flow%s", "email": "%s", "password": "password123"}
                        """.formatted(unique, email)))
                .andExpect(status().isCreated());

        return login(email);
    }

    private String signupAdmin() throws Exception {

        String token = signup();
        String email = JsonPath.read(get("/api/auth/me", token).andReturn().getResponse().getContentAsString(), "$.email");

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.saveAndFlush(user);

        // Log in again so the token carries the ADMIN role.
        return login(email);
    }

    private String login(String email) throws Exception {

        String body = mockMvc.perform(postJson("/api/auth/login", """
                        {"email": "%s", "password": "password123"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private long userId(String token) throws Exception {

        return ((Number) JsonPath.read(
                get("/api/auth/me", token).andReturn().getResponse().getContentAsString(), "$.id"
        )).longValue();
    }

    // Helper names shadow MockMvcRequestBuilders.get/post/put, so those are called qualified.

    private ResultActions get(String url, String token) throws Exception {

        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions post(String url, String token, String json) throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(url)
                .header("Authorization", "Bearer " + token);

        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }

        return mockMvc.perform(request);
    }

    private ResultActions put(String url, String token, String json) throws Exception {

        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static MockHttpServletRequestBuilder postJson(String url, String json) {

        return MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }
}