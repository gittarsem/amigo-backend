package amigo_backend.recommendation.service;

import amigo_backend.recommendation.dto.BuddyRecommendationRequestDTO;
import amigo_backend.recommendation.dto.BuddyRecommendationResponseDTO;

public interface RecommendationService {

    BuddyRecommendationResponseDTO recommendBuddies(String email, BuddyRecommendationRequestDTO request);
}