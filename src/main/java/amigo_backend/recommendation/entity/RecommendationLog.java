package amigo_backend.recommendation.entity;

import amigo_backend.entity.User;
import amigo_backend.event.entity.Event;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One buddy-recommendation request and the candidates shown (impressions).
 * Later actions (request sent, accepted, RSVP) should reference requestId.
 */
@Entity
@Table(
        name = "recommendation_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_log_request",
                        columnNames = "request_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_recommendation_log_user_event",
                        columnList = "user_id, event_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(length = 100)
    private String modelVersion;

    @Column(nullable = false)
    private boolean fallbackUsed;

    @Column(nullable = false)
    private long latencyMs;

    /** Eligible candidates considered. */
    @Column(nullable = false)
    private int candidateCount;

    /** Why ML was not used, e.g. ML_TIMEOUT or ML_HTTP_503; null when ML succeeded. */
    @Column(length = 50)
    private String errorCode;

    @ElementCollection
    @CollectionTable(
            name = "recommendation_impressions",
            joinColumns = @JoinColumn(name = "recommendation_log_id")
    )
    @Builder.Default
    private List<Impression> impressions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Impression {

        @Column(name = "rank_position", nullable = false)
        private int rankPosition;

        @Column(name = "candidate_user_id", nullable = false)
        private Long candidateUserId;

        private Double score;
    }
}