package amigo_backend.repository;

import amigo_backend.entity.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TasteProfileRepository
        extends JpaRepository<TasteProfile, Long> {

    Optional<TasteProfile> findByUserId(Long userId);

    List<TasteProfile> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserId(Long userId);
}