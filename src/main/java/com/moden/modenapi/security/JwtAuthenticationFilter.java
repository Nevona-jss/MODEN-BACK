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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // Swagger, static va boshqalar
    private static final String[] PUBLIC_PATHS = {
            "/", "/error",
            "/swagger-ui", "/swagger-ui/", "/swagger-ui/index.html",
            "/v3/api-docs", "/v3/api-docs/",
            "/swagger-resources", "/webjars"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // preflight (CORS OPTIONS) – doim ruxsat
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        final String uri = req.getRequestURI();

        // 🔓 Public endpointlarga token shart emas
        if (isPublic(uri)) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // allaqachon autentifikatsiya bo‘lsa, o‘tkazib yuboramiz
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(req, res);
                return;
            }

            String token = extractToken(req);

            // ❌ Protected endpoint + token yo‘q → 401, controllerga bormaydi
            if (token == null || token.isBlank()) {
                sendUnauthorized(res, "NO_TOKEN");
                return;
            }

            // ❌ Token yaroqsiz / expired → 401
            if (!jwtProvider.validateToken(token)) {
                sendUnauthorized(res, "INVALID_OR_EXPIRED_TOKEN");
                return;
            }

            // ✅ Token OK → userId, role dan Authentication yasash
            String userId = jwtProvider.getUserId(token);
            String roleClaim = jwtProvider.getUserRole(token);    // masalan: "CUSTOMER"
            String authority = normalizeRole(roleClaim);          // "ROLE_CUSTOMER"

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(authority));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            // Har qanday exception’da ham 401 qaytaramiz
            logger.warn("JWT filter warning: " + e.getMessage());
            sendUnauthorized(res, "INVALID_OR_EXPIRED_TOKEN");
            return;
        }

        // hammasi joyida bo‘lsa – keyingi filter/controller
        chain.doFilter(req, res);
    }

    /** 🔓 Qaysi path’lar public? */
    private boolean isPublic(String uri) {
        // 1) Swagger & static
        for (String p : PUBLIC_PATHS) {
            if (uri.equals(p) || uri.startsWith(p + "/")) return true;
        }

        if (uri.startsWith("/uploads")) {
            return true;
        }

        // 3) Auth public endpointlar
        if (uri.equals("/api/auth/login")
                || uri.equals("/api/auth/login-id")
                || uri.equals("/api/auth/refresh")) {
            return true;
        }

        // logout’ni public yoki protected qilishing o'zingga bog'liq.
        // Hoziroq public qoldirsak ham bo'ladi:
        if (uri.equals("/api/auth/logout")) {
            return true;
        }

        // 4) Telefon verifikatsiya API’lari → public
        if (uri.startsWith("/api/phone/")) {
            return true;
        }

        // 5) /api/auth/me → PROTECTED (current user info)
        if (uri.equals("/api/auth/me")) {
            return false;
        }

        return false;
    }



    private static String normalizeRole(String roleClaim) {
        if (roleClaim == null || roleClaim.isBlank()) return "ROLE_USER";
        String r = roleClaim.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    /** Authorization: Bearer ... yoki cookie(access_token) */
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

    /** 401 helper */
    private void sendUnauthorized(HttpServletResponse res, String code) throws IOException {
        if (res.isCommitted()) return;

        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        res.setContentType("application/json;charset=UTF-8");
        String body = "{\"success\":false,\"code\":\"" + code + "\",\"message\":\"Unauthorized\"}";
        res.getWriter().write(body);
        res.getWriter().flush();
    }
}
