package com.moden.modenapi.security;

import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JWT provider:
 * - Generates access/refresh tokens
 * - Validates tokens
 * - Reads subject (userId) and role(s)
 * - Exposes generic claim getters
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    // =========================
    // Generate Tokens
    // =========================

    /** Single-role variant (backward compatible) */
    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role) // keep legacy "role"
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpirationMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /** Multi-role variant (preferred if you have multiple roles) */
    public String generateAccessToken(String userId, List<String> roles) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles) // canonical "roles"
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpirationMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpirationMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    // =========================
    // Read Claims
    // =========================

    /** Subject == userId */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /** Legacy: single role (may be null). Prefer getRoles(). */
    public String getUserRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /** Roles from "roles" or "role" (string/array/comma-separated) */
    public List<String> getRoles(String token) {
        Object raw = getClaim(token, "roles");
        if (raw == null) raw = getClaim(token, "role");

        List<String> out = new ArrayList<>();
        if (raw instanceof String s) {
            for (String p : s.split("[,\\s]+")) {
                if (!p.isBlank()) out.add(p.trim());
            }
        } else if (raw instanceof Collection<?> c) {
            for (Object o : c) {
                if (o != null) {
                    String v = o.toString().trim();
                    if (!v.isBlank()) out.add(v);
                }
            }
        }
        return out;
    }

    /** Generic claim getter (Object) */
    public Object getClaim(String token, String name) {
        return parseClaims(token).get(name);
    }

    // =========================
    // Validation
    // =========================

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

    // =========================
    // Helpers
    // =========================

    /** Made public so filters/controllers can reuse it */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }


    /** Extract access token from Authorization: Bearer ... or access_token cookie */
    public String resolveToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]);
        for (Cookie c : cookies) {
            if ("access_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    /** Convenience: parse userId from request */
    public UUID getUserIdFrom(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !validateToken(token)) return null;
        return UUID.fromString(getUserId(token));
    }

    /** Convenience: parse role string from request (legacy single role) */
    public String getRoleFrom(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !validateToken(token)) return null;
        return getUserRole(token);
    }
}
