package amigo_backend.recommendation.controller;

import amigo_backend.recommendation.dto.BuddyRecommendationRequestDTO;
import amigo_backend.recommendation.dto.BuddyRecommendationResponseDTO;
import amigo_backend.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/buddies")
    public ResponseEntity<BuddyRecommendationResponseDTO> recommendBuddies(
            Authentication authentication,
            @Valid @RequestBody BuddyRecommendationRequestDTO request
    ) {
        return ResponseEntity.ok(
                recommendationService.recommendBuddies(authentication.getName(), request)
        );
    }
}