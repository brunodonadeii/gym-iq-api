package com.gymiq.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String JWT_SECRET = "12345678901234567890123456789012";

    @Test
    void generatedTokenShouldExposeExpectedClaims() {
        JwtUtil jwtUtil = jwtUtil(60_000L);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000030");

        String token = jwtUtil.generateToken("admin@gymiq.com", "ADMIN", userId);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("admin@gymiq.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        JwtUtil jwtUtil = jwtUtil(60_000L);

        assertThat(jwtUtil.validateToken("token-invalido")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void validateTokenShouldReturnFalseForExpiredToken() {
        JwtUtil jwtUtil = jwtUtil(-1_000L);
        String token = jwtUtil.generateToken(
                "admin@gymiq.com",
                "ADMIN",
                UUID.fromString("00000000-0000-0000-0000-000000000030"));

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    private JwtUtil jwtUtil(long expirationMs) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", expirationMs);
        return jwtUtil;
    }
}
