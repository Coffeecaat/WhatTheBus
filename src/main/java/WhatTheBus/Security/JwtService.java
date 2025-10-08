package WhatTheBus.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration:86400000}") // Default: 24h
    private long jwtExpiration;

    public String generateToken(String appId, String appVersion, String platform) {
        return Jwts.builder()
                .subject("app-client")
                .claim("appId", appId)
                .claim("appVersion", appVersion)
                .claim("platform", platform)
                .claim("tokenType", "CLIENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractAppId(String token) {
        return extractAllClaims(token).get("appId", String.class);
    }

    public String extractPlatform(String token) {
        return extractAllClaims(token).get("platform", String.class);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("tokenType", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date())
                    && claims.getIssuedAt().before(new Date())
                    && "CLIENT".equals(claims.get("tokenType"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractHashedClientId(String token) {
        Claims claims = extractAllClaims(token);
        String clientIdentifier = buildClientIdentifier(claims);
        return hashIdentifier(clientIdentifier);
    }

    private String buildClientIdentifier(Claims claims) {
        return claims.get("appId", String.class) + ":" +
               claims.get("platform", String.class) + ":" +
               claims.get("appVersion", String.class);
    }

    private String hashIdentifier(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}