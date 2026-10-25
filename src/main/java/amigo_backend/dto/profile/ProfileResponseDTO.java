package amigo_backend.dto.profile;

import amigo_backend.entity.User;
import amigo_backend.entity.UserProfile;
import amigo_backend.enums.Role;

import java.time.LocalDate;

public record ProfileResponseDTO(
        Long id,
        Long userId,
        String userName,
        String firstName,
        String lastName,
        String email,
        String bio,
        LocalDate dateOfBirth,
        UserProfile.Gender gender,
        String city,
        String profileImageUrl,
        boolean emailVerified,
        boolean faceVerified,
        boolean livenessVerified,
        Role role
) {
}
