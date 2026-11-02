package amigo_backend.event.service.impl;

import amigo_backend.common.exception.ApiException;
import amigo_backend.entity.User;
import amigo_backend.enums.Role;
import amigo_backend.event.dto.EventRequestDTO;
import amigo_backend.event.dto.EventResponseDTO;
import amigo_backend.event.entity.Event;
import amigo_backend.event.entity.EventVerification;
import amigo_backend.event.enums.EventStatus;
import amigo_backend.event.repository.EventRepository;
import amigo_backend.event.repository.EventVerificationRepository;
import amigo_backend.event.service.EventService;
import amigo_backend.event.service.EventVerificationService;
import amigo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String EVENT_NOT_FOUND = "Event not found";
    private static final Duration MAX_EVENT_DURATION = Duration.ofDays(30);
    private static final Pattern TAG_PATTERN = Pattern.compile("^[A-Z0-9_]{2,50}$");

    private final EventRepository eventRepository;
    private final EventVerificationRepository eventVerificationRepository;
    private final EventVerificationService eventVerificationService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public EventResponseDTO createEvent(String email, EventRequestDTO request) {

        User user = findUserByEmail(email);

        validate(request);

        Event event = Event.builder()
                .createdBy(user)
                .status(EventStatus.PENDING_REVIEW)
                .build();

        applyDetails(event, request);

        Event savedEvent = eventRepository.save(event);

        EventVerification verification = eventVerificationService.submitForReview(
                savedEvent,
                user,
                request.supportingInformation()
        );

        return EventResponseDTO.from(savedEvent, verification);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO getEvent(String email, Long eventId) {

        User user = findUserByEmail(email);
        Event event = findEvent(eventId);

        // Unapproved events are visible only to the submitter and admins. Others
        // get 404 so the event's existence is not revealed.
        if (!canManage(event, user)) {
            if (!EventStatus.PUBLICLY_VISIBLE.contains(event.getStatus())) {
                throw ApiException.notFound(EVENT_NOT_FOUND);
            }
            return EventResponseDTO.from(event, null);
        }

        return EventResponseDTO.from(event, findVerification(eventId));
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(String email, Long eventId, EventRequestDTO request) {

        User user = findUserByEmail(email);
        Event event = findEvent(eventId);

        if (!isOwner(event, user)) {
            throw ApiException.notFound(EVENT_NOT_FOUND);
        }

        if (!EventStatus.EDITABLE.contains(event.getStatus())) {
            throw ApiException.conflict("An event with status " + event.getStatus() + " cannot be edited");
        }

        if (!event.getEndTime().isAfter(Event.nowInEventZone())) {
            throw ApiException.conflict("An event that has already ended cannot be edited");
        }

        validate(request);
        applyDetails(event, request);

        // Every edit, including to an approved event, goes back to review so
        // approved details cannot be swapped for unreviewed ones.
        event.setStatus(EventStatus.PENDING_REVIEW);

        Event savedEvent = eventRepository.save(event);

        EventVerification verification = eventVerificationService.submitForReview(
                savedEvent,
                user,
                request.supportingInformation()
        );

        return EventResponseDTO.from(savedEvent, verification);
    }

    @Override
    @Transactional
    public EventResponseDTO cancelEvent(String email, Long eventId) {

        User user = findUserByEmail(email);
        Event event = findEvent(eventId);

        if (!canManage(event, user)) {
            throw ApiException.notFound(EVENT_NOT_FOUND);
        }

        if (!EventStatus.CANCELLABLE.contains(event.getStatus())) {
            throw ApiException.conflict("An event with status " + event.getStatus() + " cannot be cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);

        return EventResponseDTO.from(eventRepository.save(event), findVerification(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getApprovedEvents() {

        return eventRepository
                .findByStatusAndEndTimeAfterOrderByStartTimeAsc(
                        EventStatus.APPROVED,
                        Event.nowInEventZone()
                )
                .stream()
                .map(event -> EventResponseDTO.from(event, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getMyEvents(String email) {

        User user = findUserByEmail(email);

        return eventRepository
                .findByCreatedByIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(event -> EventResponseDTO.from(event, findVerification(event.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Event getRecommendableEvent(Long eventId) {

        Event event = findEvent(eventId);

        if (event.getStatus() != EventStatus.APPROVED) {
            throw ApiException.notFound(EVENT_NOT_FOUND);
        }

        if (!event.getEndTime().isAfter(Event.nowInEventZone())) {
            throw ApiException.conflict("This event has already ended");
        }

        return event;
    }

    /** Server-side checks that the DTO annotations cannot express. */
    private void validate(EventRequestDTO request) {

        LocalDateTime now = Event.nowInEventZone();

        if (!request.startTime().isAfter(now)) {
            throw ApiException.badRequest("Start time must be in the future");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("End time must be after the start time");
        }

        if (Duration.between(request.startTime(), request.endTime()).compareTo(MAX_EVENT_DURATION) > 0) {
            throw ApiException.badRequest("An event can last at most 30 days");
        }

        validateUrl("Official URL", request.officialUrl());
        validateUrl("Ticket URL", request.ticketUrl());
        validateUrl("Poster URL", request.posterUrl());
    }

    private void validateUrl(String field, String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);

            if ((scheme.equals("http") || scheme.equals("https")) &&
                    uri.getHost() != null && !uri.getHost().isBlank()) {
                return;
            }
        } catch (URISyntaxException ignored) {
            // fall through to the error below
        }

        throw ApiException.badRequest(field + " must be a valid http or https URL");
    }

    /** Upper-cases tags and turns spaces/hyphens into underscores ("music festivals" -> MUSIC_FESTIVALS). */
    private Set<String> normalizeTags(List<String> tags) {

        Set<String> normalized = new LinkedHashSet<>();

        if (tags == null) {
            return normalized;
        }

        for (String tag : tags) {

            String value = tag == null
                    ? ""
                    : tag.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");

            if (!TAG_PATTERN.matcher(value).matches()) {
                throw ApiException.badRequest(
                        "Invalid tag '" + tag + "': use 2-50 letters, digits or underscores"
                );
            }

            normalized.add(value);
        }

        return normalized;
    }

    private void applyDetails(Event event, EventRequestDTO request) {

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setCategory(request.category());
        event.getTags().clear();
        event.getTags().addAll(normalizeTags(request.tags()));
        event.setVenueName(request.venueName().trim());
        event.setAddress(request.address().trim());
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setBudget(request.budget());
        event.setOrganizerName(trimToNull(request.organizerName()));
        event.setOfficialUrl(trimToNull(request.officialUrl()));
        event.setTicketUrl(trimToNull(request.ticketUrl()));
        event.setPosterUrl(trimToNull(request.posterUrl()));
    }

    private EventVerification findVerification(Long eventId) {

        return eventVerificationRepository.findByEventId(eventId).orElse(null);
    }

    private Event findEvent(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound(EVENT_NOT_FOUND));
    }

    private boolean isOwner(Event event, User user) {

        return event.getCreatedBy().getId().equals(user.getId());
    }

    private boolean canManage(Event event, User user) {

        return isOwner(event, user) || user.getRole() == Role.ADMIN;
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    static String trimToNull(String value) {

        return value == null || value.isBlank() ? null : value.trim();
    }
}