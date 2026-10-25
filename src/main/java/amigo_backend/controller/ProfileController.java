package amigo_backend.controller;

import amigo_backend.dto.profile.ProfileResponseDTO;
import amigo_backend.dto.profile.UpdateProfileRequestDTO;
import amigo_backend.service.ProfileService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponseDTO> getProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                profileService.getProfile(
                        authentication.getName()
                )
        );
    }

    @PutMapping
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDTO request
    ) {

        return ResponseEntity.ok(
                profileService.updateProfile(
                        authentication.getName(),
                        request
                )
        );
    }
}
