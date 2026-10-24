package amigo_backend.dto.auth;

public record AuthResponse(
        String accessToken,
        UserResponse userResponse
) {
}
