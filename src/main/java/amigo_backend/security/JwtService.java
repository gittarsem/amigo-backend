package amigo_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // ============================================================
    // ACCESS TOKEN
    // ============================================================

    public String generateAccessToken(String email, String role) {

        return generateToken(
                email,
                role,
                "ACCESS",
                accessTokenExpiration
        );
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    public String generateRefreshToken(String email, String role) {

        return generateToken(
                email,
                role,
                "REFRESH",
                refreshTokenExpiration
        );
    }

    // ============================================================
    // GENERATE TOKEN
    // ============================================================

    private String generateToken(
            String email,
            String role,
            String tokenType,
            long expiration
    ) {

        Date now = new Date();

        return Jwts.builder()
                .subject(email)

                // Custom claims
                .claim("role", role)
                .claim("type", tokenType)

                // Standard claims
                .issuedAt(now)
                .expiration(
                        new Date(now.getTime() + expiration)
                )

                // Sign token
                .signWith(secretKey)

                .compact();
    }

    // ============================================================
    // EXTRACT EMAIL
    // ============================================================

    public String extractEmail(String token) {

        return extractClaims(token)
                .getSubject();
    }

    // ============================================================
    // EXTRACT ROLE
    // ============================================================

    public String extractRole(String token) {

        return extractClaims(token)
                .get("role", String.class);
    }

    // ============================================================
    // EXTRACT TOKEN TYPE
    // ============================================================

    public String extractTokenType(String token) {

        return extractClaims(token)
                .get("type", String.class);
    }

    // ============================================================
    // CHECK ACCESS TOKEN
    // ============================================================

    public boolean isAccessToken(String token) {

        try {

            return "ACCESS".equals(
                    extractTokenType(token)
            );

        } catch (Exception e) {

            return false;
        }
    }

    // ============================================================
    // CHECK REFRESH TOKEN
    // ============================================================

    public boolean isRefreshToken(String token) {

        try {

            return "REFRESH".equals(
                    extractTokenType(token)
            );

        } catch (Exception e) {

            return false;
        }
    }

    // ============================================================
    // VALIDATE TOKEN
    // ============================================================

    public boolean isValid(String token) {

        try {

            extractClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // ============================================================
    // EXTRACT CLAIMS
    // ============================================================

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}