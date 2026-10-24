package amigo_backend.dto.auth;

import amigo_backend.enums.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean emailVerified,
        boolean faceVerified,
        boolean livenessVerified
) {
}
