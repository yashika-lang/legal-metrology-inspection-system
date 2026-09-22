package com.legalmetrology.security;

import com.legalmetrology.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hmac-sha-256-signing",
                900_000L,
                604_800_000L,
                "legal-metrology-inspection-system-test"
        );
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generatesATokenThatIsImmediatelyValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(
                userId, "inspector@example.gov.in", List.of(new SimpleGrantedAuthority("ROLE_INSPECTOR")));

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    }

    @Test
    void extractsTheOriginalClaimsFromTheToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(
                userId, "inspector@example.gov.in", List.of(new SimpleGrantedAuthority("ROLE_INSPECTOR")));

        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("inspector@example.gov.in");
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactly("ROLE_INSPECTOR");
    }

    @Test
    void rejectsAMalformedToken() {
        assertThat(jwtTokenProvider.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(new JwtProperties(
                "a-completely-different-secret-key-for-this-one-test-case",
                900_000L, 604_800_000L, "legal-metrology-inspection-system-test"));

        String tokenFromOtherIssuer = otherProvider.generateAccessToken(
                UUID.randomUUID(), "someone@example.gov.in", List.of(new SimpleGrantedAuthority("ROLE_INSPECTOR")));

        assertThat(jwtTokenProvider.isTokenValid(tokenFromOtherIssuer)).isFalse();
    }
}
