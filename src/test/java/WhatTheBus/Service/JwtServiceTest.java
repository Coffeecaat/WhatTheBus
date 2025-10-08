package WhatTheBus.Service;

import WhatTheBus.Security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "VGhpcyBpcyBhIHNhbXBsZSBzZWNyZXQga2V5IGZvciBKV1QgdGVzdGluZyBwdXJwb3Nlcy4gSXQgaXMgYXQgbGVhc3QgMjU2IGJpdHMgbG9uZyBmb3IgSFMyNTYgYWxnb3JpdGht";
    private final long jwtExpiration = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", jwtExpiration);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void extractHashedClientId_shouldReturnHashedValue() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");
        String hashedClientId = jwtService.extractHashedClientId(token);

        assertNotNull(hashedClientId);
        assertFalse(hashedClientId.isEmpty());
        // Hash should be consistent for same input
        String hashedClientId2 = jwtService.extractHashedClientId(token);
        assertEquals(hashedClientId, hashedClientId2);
    }

    @Test
    void extractAppId_shouldReturnCorrectAppId() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");

        String appId = jwtService.extractAppId(token);

        assertEquals("WhatTheBus", appId);
    }

    @Test
    void extractPlatform_shouldReturnCorrectPlatform() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "iOS");

        String platform = jwtService.extractPlatform(token);

        assertEquals("iOS", platform);
    }

    @Test
    void extractTokenType_shouldReturnClient() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");

        String tokenType = jwtService.extractTokenType(token);

        assertEquals("CLIENT", tokenType);
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");

        boolean isValid = jwtService.isTokenValid(token);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_shouldReturnFalse_forInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtService.isTokenValid(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void extractAllClaims_shouldReturnValidClaims() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");
        Claims claims = jwtService.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals("app-client", claims.getSubject());
        assertEquals("WhatTheBus", claims.get("appId"));
        assertEquals("1.0.0", claims.get("appVersion"));
        assertEquals("Android", claims.get("platform"));
        assertEquals("CLIENT", claims.get("tokenType"));
    }

    @Test
    void hashIdentifier_shouldProduceDifferentHashesForDifferentInputs() {
        String token1 = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");
        String token2 = jwtService.generateToken("WhatTheBus", "1.0.0", "iOS");

        String hash1 = jwtService.extractHashedClientId(token1);
        String hash2 = jwtService.extractHashedClientId(token2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateToken_shouldIncludeExpirationTime() {
        String token = jwtService.generateToken("WhatTheBus", "1.0.0", "Android");
        Claims claims = jwtService.extractAllClaims(token);

        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        assertNotNull(expiration);
        assertNotNull(issuedAt);
        assertTrue(expiration.after(issuedAt));

        // Check that expiration is approximately 24 hours from issuedAt
        long timeDiff = expiration.getTime() - issuedAt.getTime();
        assertTrue(Math.abs(timeDiff - jwtExpiration) < 1000); // Allow 1 second tolerance
    }

    @Test
    void isTokenValid_shouldReturnFalse_forNullToken() {
        boolean isValid = jwtService.isTokenValid(null);

        assertFalse(isValid);
    }

    @Test
    void isTokenValid_shouldReturnFalse_forEmptyToken() {
        boolean isValid = jwtService.isTokenValid("");

        assertFalse(isValid);
    }
}