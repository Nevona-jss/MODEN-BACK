package com.moden.modenapi.security;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Utility class for generating and validating JWT tokens.
 * <p>
 * Supports both Access and Refresh tokens, each with configurable expiration.
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    /**
     * Generates a signed JWT access token.
     *
     * @param userId unique user identifier
     * @param role   user's role (CUSTOMER, DESIGNER, HAIR_STUDIO, ADMIN)
     * @return signed JWT string
     */
    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpirationMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /**
     * Generates a signed JWT refresh token.
     *
     * @param userId unique user identifier
     * @return signed JWT string
     */
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpirationMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /**
     * Parses the token and returns the user ID (subject).
     *
     * @param token JWT string
     * @return userId (subject)
     */
    public String getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * Extracts the user role from the token claims.
     *
     * @param token JWT string
     * @return role name (e.g. ADMIN, DESIGNER, CUSTOMER)
     */
    public String getUserRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Validates the provided JWT token.
     * <p>
     * Checks signature validity, expiration date, and token structure.
     *
     * @param token JWT string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret())
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("❌ JWT expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("❌ Unsupported JWT: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("❌ Malformed JWT: " + e.getMessage());
        } catch (SignatureException e) {
            System.err.println("❌ Invalid signature: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Empty or null JWT: " + e.getMessage());
        }
        return false;
    }

    /**
     * Internal helper to parse and return claims.
     *
     * @param token JWT string
     * @return parsed Claims object
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }
}
