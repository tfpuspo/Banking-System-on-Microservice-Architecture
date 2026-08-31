package com.banking.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Issues and verifies access tokens (JWTs).
 *
 * NOTE for later: this uses HS256 with a shared secret, the simplest option to
 * get running locally. In production you'd typically switch to RS256 (a
 * private/public key pair) so other services can verify tokens using only a
 * public key, without needing the secret that can create new tokens.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-seconds:900}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.ttlSeconds = ttlSeconds;
    }

    public String issueAccessToken(UUID userId, Set<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public record ParsedToken(String userId, List<String> roles) {}

    /** Returns null if the token is invalid, tampered with, or expired. */
    public ParsedToken verify(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            return new ParsedToken(claims.getSubject(), roles);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
