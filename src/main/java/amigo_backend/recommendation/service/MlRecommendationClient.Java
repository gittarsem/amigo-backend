package amigo_backend.recommendation.service;

import amigo_backend.recommendation.dto.MlRecommendationRequestDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/** HTTP communication with the ML service's /v1/match/recommend. Nothing else. */
@Slf4j
@Component
public class MlRecommendationClient {

    private static final String RECOMMEND_PATH = "/v1/match/recommend";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private final RestClient restClient;

    public MlRecommendationClient(
            @Value("${ml.service.base-url:}") String baseUrl,
            @Value("${ml.service.token:}") String token,
            @Value("${ml.service.timeout:1s}") Duration timeout
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("ml.service.base-url is not set: buddy recommendations will always use the fallback");
            this.restClient = null;
            return;
        }

        URI uri = URI.create(baseUrl.trim());

        if (!"https".equalsIgnoreCase(uri.getScheme()) && !LOOPBACK_HOSTS.contains(uri.getHost())) {
            log.warn("ML service URL is not HTTPS; the service token would be sent in clear text");
        }

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build()
        );
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl.trim())
                // Buffering sets Content-Length; the ML service rejects requests without it (411).
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory));

        if (token != null && !token.isBlank()) {
            builder.defaultHeader("X-Service-Token", token.trim());
        }

        this.restClient = builder.build();
    }

    public boolean isEnabled() {

        return restClient != null;
    }

    /**
     * @throws MlServiceException when the ML service is not configured,
     *                            unreachable, slow, or returns an error
     */
    public MlResponse recommend(MlRecommendationRequestDTO request, String requestId) {

        if (restClient == null) {
            throw new MlServiceException("ML_NOT_CONFIGURED", "ML service URL is not configured");
        }

        try {
            MlResponse response = restClient.post()
                    .uri(RECOMMEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Request-Id", requestId)
                    .body(request)
                    .retrieve()
                    .body(MlResponse.class);

            if (response == null || response.recommendations() == null) {
                throw new MlServiceException("ML_BAD_RESPONSE", "ML response had no recommendations");
            }

            return response;

        } catch (RestClientResponseException exception) {
            throw new MlServiceException(
                    "ML_HTTP_" + exception.getStatusCode().value(),
                    "ML service returned " + exception.getStatusCode().value()
            );
        } catch (ResourceAccessException exception) {
            boolean timeout = exception.getCause() instanceof HttpTimeoutException;
            throw new MlServiceException(
                    timeout ? "ML_TIMEOUT" : "ML_UNAVAILABLE",
                    "ML service could not be reached: " + exception.getMessage()
            );
        } catch (RestClientException exception) {
            throw new MlServiceException("ML_BAD_RESPONSE", "ML response could not be read: " + exception.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MlResponse(
            List<Recommendation> recommendations,
            // Not returned by the ML service today; read if it starts sending them.
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("request_id") String requestId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recommendation(
            Long candidateUserId,
            Double score
    ) {
    }

    @Getter
    public static class MlServiceException extends RuntimeException {

        /** Short code stored with the recommendation log, e.g. ML_TIMEOUT, ML_HTTP_503. */
        private final String errorCode;

        public MlServiceException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}