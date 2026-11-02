package amigo_backend.recommendationprofile.controller;

import amigo_backend.recommendationprofile.dto.RecommendationProfileDTO;
import amigo_backend.recommendationprofile.service.RecommendationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendation-profile")
@RequiredArgsConstructor
public class RecommendationProfileController {

    private final RecommendationProfileService recommendationProfileService;

    @GetMapping
    public ResponseEntity<RecommendationProfileDTO> getProfile(Authentication authentication) {

        return ResponseEntity.ok(recommendationProfileService.getProfile(authentication.getName()));
    }

    @PutMapping
    public ResponseEntity<RecommendationProfileDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody RecommendationProfileDTO request
    ) {
        return ResponseEntity.ok(
                recommendationProfileService.updateProfile(authentication.getName(), request)
        );
    }

    @DeleteMapping
    public ResponseEntity<RecommendationProfileDTO> resetProfile(Authentication authentication) {

        return ResponseEntity.ok(recommendationProfileService.resetProfile(authentication.getName()));
    }
}