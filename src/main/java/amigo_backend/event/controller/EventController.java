package amigo_backend.event.controller;

import amigo_backend.event.dto.EventPreferenceDTO;
import amigo_backend.event.dto.EventRequestDTO;
import amigo_backend.event.dto.EventResponseDTO;
import amigo_backend.event.service.EventPreferenceService;
import amigo_backend.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventPreferenceService eventPreferenceService;

    @PostMapping("/events")
    public ResponseEntity<EventResponseDTO> createEvent(
            Authentication authentication,
            @Valid @RequestBody EventRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEvent(authentication.getName(), request));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponseDTO> getEvent(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.getEvent(authentication.getName(), id));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EventRequestDTO request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(authentication.getName(), id, request));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<EventResponseDTO> cancelEvent(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.cancelEvent(authentication.getName(), id));
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponseDTO>> getApprovedEvents() {

        return ResponseEntity.ok(eventService.getApprovedEvents());
    }

    @GetMapping("/users/me/events")
    public ResponseEntity<List<EventResponseDTO>> getMyEvents(Authentication authentication) {

        return ResponseEntity.ok(eventService.getMyEvents(authentication.getName()));
    }

    // ---- Event-specific preferences ----

    @GetMapping("/events/preference-questions")
    public ResponseEntity<List<EventPreferenceService.Question>> getPreferenceQuestions() {

        return ResponseEntity.ok(eventPreferenceService.getQuestions());
    }

    @GetMapping("/events/{id}/preferences")
    public ResponseEntity<EventPreferenceDTO> getMyPreferences(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventPreferenceService.getMyPreferences(authentication.getName(), id));
    }

    @PutMapping("/events/{id}/preferences")
    public ResponseEntity<EventPreferenceDTO> savePreferences(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EventPreferenceDTO request
    ) {
        return ResponseEntity.ok(eventPreferenceService.savePreferences(authentication.getName(), id, request));
    }

    @DeleteMapping("/events/{id}/preferences")
    public ResponseEntity<Void> deletePreferences(
            Authentication authentication,
            @PathVariable Long id
    ) {
        eventPreferenceService.deletePreferences(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}