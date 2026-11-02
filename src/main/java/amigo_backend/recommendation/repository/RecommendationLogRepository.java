package amigo_backend.recommendation.repository;

import amigo_backend.recommendation.entity.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationLogRepository
        extends JpaRepository<RecommendationLog, Long> {

    Optional<RecommendationLog> findByRequestId(String requestId);
}