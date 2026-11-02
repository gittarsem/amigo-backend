package amigo_backend.event.entity;

import amigo_backend.entity.User;
import amigo_backend.enums.InterestCategory;
import amigo_backend.event.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(
                        name = "idx_events_status_start_time",
                        columnList = "status, start_time"
                ),
                @Index(
                        name = "idx_events_created_by",
                        columnList = "created_by"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    /** Event times are local date-times in IST, matching the ML service convention. */
    public static final ZoneId EVENT_ZONE = ZoneId.of("Asia/Kolkata");

    public static LocalDateTime nowInEventZone() {
        return LocalDateTime.now(EVENT_ZONE);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterestCategory category;

    @ElementCollection
    @CollectionTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "tag", nullable = false, length = 50)
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @Column(nullable = false, length = 200)
    private String venueName;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(length = 150)
    private String organizerName;

    @Column(length = 1000)
    private String officialUrl;

    @Column(length = 1000)
    private String ticketUrl;

    @Column(length = 1000)
    private String posterUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventStatus status;

    @Version
    private Long version;

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