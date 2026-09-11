package com.swiftroute.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        // Set 256-bit test secret and expiration times via reflection
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationMs", 3600000L); // 1 hour
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationMs", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("Generate access token and verify claims extraction")
    void testGenerateAndValidateAccessToken() {
        UserPrincipal principal = new UserPrincipal(
                42L,
                "dispatcher_sarah",
                "sarah@swiftroute.io",
                "Sarah Jenkins",
                "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_DISPATCHER"))
        );

        String token = tokenProvider.generateAccessToken(principal);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals("dispatcher_sarah", tokenProvider.getUsernameFromToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Generate refresh token and verify validity")
    void testGenerateRefreshToken() {
        UserPrincipal principal = new UserPrincipal(
                10L,
                "tech_dave",
                "dave@swiftroute.io",
                "Dave Miller",
                "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))
        );

        String refreshToken = tokenProvider.generateRefreshToken(principal);
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertEquals("tech_dave", tokenProvider.getUsernameFromToken(refreshToken));
    }

    @Test
    @DisplayName("Validate returns false for corrupted or invalid tokens")
    void testInvalidTokenValidation() {
        assertFalse(tokenProvider.validateToken("invalid.token.payload"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }
}
