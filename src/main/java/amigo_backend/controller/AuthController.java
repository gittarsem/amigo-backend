package amigo_backend.controller;

import amigo_backend.service.AuthService;
import amigo_backend.dto.auth.AuthResponse;
import amigo_backend.dto.auth.LoginRequestDTO;
import amigo_backend.dto.auth.SignupRequestDTO;

import amigo_backend.dto.auth.UserResponse;
import amigo_backend.entity.User;
import amigo_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;


    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequestDTO request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.signup(
                request,
                response
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(
                request,
                response
        );

        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );

        UserResponse response = new UserResponse(
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

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(
                    name = "refreshToken",
                    required = false
            ) String refreshToken
    ) {
        return ResponseEntity.ok(
                authService.refresh(refreshToken)
        );
    }
}

