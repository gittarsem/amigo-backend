package amigo_backend.event.controller;

import amigo_backend.event.dto.AdminEventDecisionRequestDTO;
import amigo_backend.event.dto.AdminEventResponseDTO;
import amigo_backend.event.service.EventVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin review. Requires the ADMIN role (SecurityConfig, re-checked in the service). */
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventVerificationService eventVerificationService;

    @GetMapping("/pending")
    public ResponseEntity<List<AdminEventResponseDTO>> getPendingEvents(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                eventVerificationService.getPendingEvents(authentication.getName())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminEventResponseDTO> getEvent(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                eventVerificationService.getEventForReview(authentication.getName(), id)
        );
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminEventResponseDTO> approve(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminEventDecisionRequestDTO request
    ) {
        return ResponseEntity.ok(
                eventVerificationService.approve(authentication.getName(), id, request)
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminEventResponseDTO> reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminEventDecisionRequestDTO request
    ) {
        return ResponseEntity.ok(
                eventVerificationService.reject(authentication.getName(), id, request)
        );
    }

    @PostMapping("/{id}/request-information")
    public ResponseEntity<AdminEventResponseDTO> requestInformation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminEventDecisionRequestDTO request
    ) {
        return ResponseEntity.ok(
                eventVerificationService.requestInformation(authentication.getName(), id, request)
        );
    }
}