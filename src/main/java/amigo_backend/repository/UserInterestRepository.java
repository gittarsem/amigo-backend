package amigo_backend.repository;


import amigo_backend.entity.User;
import amigo_backend.entity.UserInterest;
import amigo_backend.enums.InterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInterestRepository
        extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUser(User user);

    boolean existsByUserAndTag(
            User user,
            InterestTag tag
    );

    void deleteByUser(User user);
}
