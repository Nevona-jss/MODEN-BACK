package com.moden.modenapi.common.utils;

import jakarta.servlet.http.HttpServletRequest;

public class JwtUtils {
    public static String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
