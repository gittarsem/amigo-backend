package amigo_backend.repository;

import amigo_backend.entity.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TasteProfileRepository
        extends JpaRepository<TasteProfile, Long> {

    Optional<TasteProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}