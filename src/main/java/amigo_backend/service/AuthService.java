package amigo_backend.service;

import amigo_backend.dto.auth.AuthResponse;
import amigo_backend.dto.auth.LoginRequestDTO;
import amigo_backend.dto.auth.SignupRequestDTO;
import amigo_backend.dto.auth.UserResponse;
import amigo_backend.entity.User;
import amigo_backend.enums.Role;
import amigo_backend.repository.UserRepository;
import amigo_backend.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signup(
            SignupRequestDTO request,
            HttpServletResponse response
    ) {

        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .userName(request.userName())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .emailVerified(false)
                .faceVerified(false)
                .livenessVerified(false)
                .build();
        System.out.println("Signup username = [" + request.userName() + "]");
        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        addRefreshTokenCookie(response, refreshToken);

        return new AuthResponse(
                accessToken,
                mapToUserResponse(savedUser)
        );
    }

    public AuthResponse login(
            LoginRequestDTO request,
            HttpServletResponse response
    ) {

        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getEmail(),
                user.getRole().name()
        );

        addRefreshTokenCookie(response, refreshToken);

        return new AuthResponse(
                accessToken,
                mapToUserResponse(user)
        );
    }

    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken
    ) {

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(
                "Set-Cookie",
                cookie.toString()
        );
    }

    private UserResponse mapToUserResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isEmailVerified(),
                user.isFaceVerified(),
                user.isLivenessVerified()
        );
    }

    public AuthResponse refresh(
            String refreshToken
    ) {
        if (refreshToken == null ||
                !jwtService.isValid(refreshToken) ||
                !jwtService.isRefreshToken(refreshToken)) {

            throw new IllegalArgumentException(
                    "Invalid or expired refresh token"
            );
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                accessToken,
                mapToUserResponse(user)
        );
    }
}