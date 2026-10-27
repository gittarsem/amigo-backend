package amigo_backend.entity;

import amigo_backend.enums.TasteDimension;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "taste_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_taste_profile_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TasteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean initialized = false;

    @ElementCollection
    @CollectionTable(
            name = "taste_dimension_scores",
            joinColumns = @JoinColumn(name = "taste_profile_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "dimension")
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Map<TasteDimension, Double> dimensionScores = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "taste_dimension_confidence",
            joinColumns = @JoinColumn(name = "taste_profile_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "dimension")
    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private Map<TasteDimension, Double> dimensionConfidence = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "taste_tag_scores",
            joinColumns = @JoinColumn(name = "taste_profile_id")
    )
    @MapKeyColumn(name = "tag")
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Map<String, Double> tagScores = new HashMap<>();
}