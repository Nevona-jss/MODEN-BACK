package com.moden.modenapi.security;

import com.moden.modenapi.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static com.moden.modenapi.security.SecurityConstants.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();

        // 🔓 Swagger va static resource’lar JWT tekshiruvisiz o‘tadi
        if (uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars")
                || uri.equals("/")
                || uri.startsWith("/api/auth") // login/signup uchun ham JWT kerak emas
        ) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // 🔹 Access tokenni cookie'dan olish
            String token = extractTokenFromCookies(req);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String subject = jwtProvider.parseSubject(token);

                if (subject != null) {
                    var userDetails = userDetailsService.loadUserByUsername(subject);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT check failed: " + e.getMessage());
        }

        chain.doFilter(req, res);
    }

    /**
     * 🍪 Cookie ichidan access_token ni ajratib olish
     */
    private String extractTokenFromCookies(HttpServletRequest req) {
        // 1️⃣ Header orqali
        String headerToken = JwtUtils.extractToken(req);
        if (headerToken != null) {
            return headerToken;
        }

        // 2️⃣ Cookie orqali
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
