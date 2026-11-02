package amigo_backend.recommendationprofile.entity;

import amigo_backend.entity.User;
import amigo_backend.recommendationprofile.enums.RecommendationDimension;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * A user's default buddy-matching weights. Event-specific adjustments are
 * applied on top at request time and never written back here.
 */
@Entity
@Table(
        name = "recommendation_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_profile_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ElementCollection
    @CollectionTable(
            name = "recommendation_profile_weights",
            joinColumns = @JoinColumn(name = "recommendation_profile_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "dimension", length = 30)
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Map<RecommendationDimension, Double> weights =
            new EnumMap<>(RecommendationDimension.class);

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}