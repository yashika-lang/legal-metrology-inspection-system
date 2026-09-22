package com.legalmetrology.security;

import com.legalmetrology.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates the short-lived JWT access token. Refresh tokens are
 * a separate, opaque, server-persisted concept (see {@code RefreshToken})
 * so they can be revoked — this class only deals with the stateless access
 * token that is verified on every request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    /** HMAC-SHA256 requires a >=256-bit (32-byte) key; a shorter secret would otherwise only fail at first-token-generation via a runtime WeakKeyException. */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    @PostConstruct
    void validateSecretStrength() {
        int actualBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8).length;
        if (actualBytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES
                    + " bytes for HMAC-SHA256 signing; configured secret is only " + actualBytes + " bytes");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, List<? extends GrantedAuthority> authorities) {
        long nowMs = System.currentTimeMillis();
        Date issuedAt = new Date(nowMs);
        Date expiry = new Date(nowMs + jwtProperties.accessTokenExpirationMs());

        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(jwtProperties.issuer())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(issuedAt)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return (List<String>) parseClaims(token).get(CLAIM_ROLES);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
