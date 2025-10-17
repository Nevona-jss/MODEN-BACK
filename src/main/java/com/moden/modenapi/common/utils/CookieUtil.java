package com.moden.modenapi.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    public static void setRefreshTokenCookie(
            HttpServletResponse response,
            HttpServletRequest request,
            String refreshToken
    ) {
        Duration maxAge = Duration.ofDays(30);

        String host = request.getHeader("host");
        boolean isLocal = host != null &&
                (host.contains("localhost") || host.contains("127.0.0.1") || host.startsWith("192.168."));

        // ✅ detect HTTPS environment
        boolean isHttps = request.isSecure() || (!isLocal && host != null && host.startsWith("https"));

        String sameSite = "None";
        boolean secure = isHttps; // must be true on HTTPS

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        System.out.printf("✅ Set refresh_token cookie [secure=%s, sameSite=%s, host=%s]%n",
                secure, sameSite, host);
    }

}
