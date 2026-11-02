package amigo_backend.event.entity;

import amigo_backend.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * What a user wants from one approved event. Saving it also opts the user in
 * to buddy matching for that event. Kept separate from the Taste Profile.
 */
@Entity
@Table(
        name = "event_specific_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_preference_user_event",
                        columnNames = {"user_id", "event_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSpecificPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Question id -> chosen option id. */
    @ElementCollection
    @CollectionTable(
            name = "event_preference_answers",
            joinColumns = @JoinColumn(name = "preference_id")
    )
    @MapKeyColumn(name = "question_id", length = 50)
    @Column(name = "option_id", nullable = false, length = 50)
    @Builder.Default
    private Map<String, String> answers = new HashMap<>();

    /** Where the user is coming from, rounded to ~1 km. Optional; used for distance. */
    private Double originLatitude;

    private Double originLongitude;

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