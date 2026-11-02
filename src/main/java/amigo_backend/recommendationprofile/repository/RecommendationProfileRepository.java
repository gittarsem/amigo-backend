package amigo_backend.recommendationprofile.repository;

import amigo_backend.recommendationprofile.entity.RecommendationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationProfileRepository
        extends JpaRepository<RecommendationProfile, Long> {

    Optional<RecommendationProfile> findByUserId(Long userId);
}