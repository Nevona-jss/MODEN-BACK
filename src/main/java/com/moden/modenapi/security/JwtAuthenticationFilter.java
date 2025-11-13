package com.moden.modenapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT Auth Filter:
 * - Skips swagger and truly public endpoints
 * - Extracts token from Authorization: Bearer ... or cookie(access_token)
 * - Validates token, sets principal=userId and ROLE_* authorities
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // Public endpoints (no auth needed)
    private static final String[] PUBLIC_PATHS = {
            "/", "/error",
            "/swagger-ui", "/swagger-ui/", "/swagger-ui/index.html",
            "/v3/api-docs", "/v3/api-docs/",
            "/swagger-resources", "/webjars"
    };

    // Public auth endpoints — DO NOT include /api/auth/me here
    private static final String[] PUBLIC_AUTH_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/admin/login"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Always allow preflight
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        final String uri = req.getRequestURI();

        if (isPublic(uri)) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // If authentication already set, continue
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(req, res);
                return;
            }

            String token = extractToken(req);
            if (token == null) {
                chain.doFilter(req, res);
                return;
            }

            if (jwtProvider.validateToken(token)) {
                // subject = userId (UUID string)
                String userId = jwtProvider.getUserId(token);

                // role claim (single). If you later add multiple roles, make this a list.
                String roleClaim = jwtProvider.getUserRole(token); // e.g. "CUSTOMER" or "ROLE_CUSTOMER"
                String authority = normalizeRole(roleClaim);       // -> "ROLE_CUSTOMER" (default ROLE_USER)

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(authority));

                // Principal = userId string (no DB lookup here)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            logger.warn("JWT filter warning: " + e.getMessage());
            // keep going to let the entrypoint/handlers respond properly
        }

        chain.doFilter(req, res);
    }

    private boolean isPublic(String uri) {
        // Swagger & static
        for (String p : PUBLIC_PATHS) {
            if (uri.equals(p) || uri.startsWith(p + "/")) return true;
        }
        // Specific public auth endpoints
        for (String p : PUBLIC_AUTH_PATHS) {
            if (uri.equals(p)) return true;
        }
        return false;
    }

    private static String normalizeRole(String roleClaim) {
        if (roleClaim == null || roleClaim.isBlank()) return "ROLE_USER";
        String r = roleClaim.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    /** Authorization: Bearer ... or cookie(access_token) */
    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}