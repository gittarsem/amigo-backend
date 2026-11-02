package amigo_backend.event.enums;

import java.util.Set;

public enum EventStatus {

    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    NEEDS_INFORMATION,
    CANCELLED,
    COMPLETED;

    /** Statuses any authenticated user may see; everything else is owner/admin only. */
    public static final Set<EventStatus> PUBLICLY_VISIBLE =
            Set.of(APPROVED, COMPLETED);

    /** Statuses in which the owner may still edit the event. */
    public static final Set<EventStatus> EDITABLE =
            Set.of(PENDING_REVIEW, NEEDS_INFORMATION, APPROVED);

    /** Statuses from which the event can be cancelled. */
    public static final Set<EventStatus> CANCELLABLE =
            Set.of(PENDING_REVIEW, NEEDS_INFORMATION, APPROVED);
}