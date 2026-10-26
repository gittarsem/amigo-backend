package amigo_backend.controller;


import amigo_backend.dto.interset.InterestsResponseDTO;
import amigo_backend.dto.interset.UpdateInterestsRequestDTO;
import amigo_backend.service.InterestService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    public ResponseEntity<InterestsResponseDTO> getInterests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                interestService.getInterests(
                        authentication.getName()
                )
        );
    }

    @PutMapping
    public ResponseEntity<InterestsResponseDTO> updateInterests(
            Authentication authentication,
            @Valid @RequestBody UpdateInterestsRequestDTO request
    ) {
        return ResponseEntity.ok(
                interestService.updateInterests(
                        authentication.getName(),
                        request
                )
        );
    }
}
