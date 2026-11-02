package amigo_backend.event.entity;

import amigo_backend.entity.User;
import amigo_backend.event.enums.VerificationDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Admin review record, kept separate from {@link Event} so the decision and
 * who made it stay auditable.
 */
@Entity
@Table(
        name = "event_verifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_verification_event",
                        columnNames = "event_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationDecision decision;

    // Evidence: snapshot of what was submitted, taken on every (re)submission.

    @Column(length = 1000)
    private String evidenceOfficialUrl;

    @Column(length = 1000)
    private String evidenceTicketUrl;

    @Column(length = 1000)
    private String evidencePosterUrl;

    @Column(length = 2000)
    private String evidenceSupportingInformation;

    /** Internal review notes. Never returned to ordinary users. */
    @Column(length = 2000)
    private String adminNotes;

    /** Shown to the submitter: rejection reason or information needed. */
    @Column(length = 1000)
    private String messageToSubmitter;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Version
    private Long version;
}