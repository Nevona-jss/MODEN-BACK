package com.moden.modenapi.security;

import com.moden.modenapi.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Custom JWT authentication filter executed once per request.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Extract JWT token from the Authorization header or cookies.</li>
 *     <li>Validate and parse the token using {@link JwtProvider}.</li>
 *     <li>Load user details and set the authenticated context for the request.</li>
 *     <li>Skip authentication for Swagger and Auth endpoints.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();

        // 🔓 Allow unauthenticated access to Swagger & Auth endpoints
        if (uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars")
                || uri.equals("/")
                || uri.startsWith("/api/auth")) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // Extract JWT from header or cookies
            String token = extractToken(req);

            if (token != null && jwtProvider.validateToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Parse userId and role from token
                String userId = jwtProvider.getUserId(token);
                String role = jwtProvider.getUserRole(token);

                var userDetails = userDetailsService.loadUserByUsername(userId);

                // Build authentication object with user role
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            logger.warn("⚠️ JWT validation failed: " + e.getMessage());
        }

        chain.doFilter(req, res);
    }

    /**
     * Extracts token from Authorization header or cookies.
     *
     * @param req current HTTP request
     * @return JWT token string or null if not found
     */
    private String extractToken(HttpServletRequest req) {
        // 1️⃣ From Authorization header
        String headerToken = JwtUtils.extractToken(req);
        if (headerToken != null) return headerToken;

        // 2️⃣ From cookies (e.g., when used with web clients)
        if (req.getCookies() != null) {
            for (var cookie : req.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
