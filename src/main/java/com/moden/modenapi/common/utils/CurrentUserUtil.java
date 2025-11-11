package com.moden.modenapi.common.utils;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class CurrentUserUtil {
    private CurrentUserUtil() {}

    /** Kim kirgan bo'lsa — o'sha foydalanuvchining ID sini (UUID) qaytaradi. */
    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        // 1) Agar principal UserDetails bo'lsa
        Object principal = auth.getPrincipal();
        String idStr;
        if (principal instanceof UserDetails ud) {
            idStr = ud.getUsername();         // siz username ga userId (UUID) saqlayotgan bo'lasiz
        } else {
            // 2) Aks holda Authentication#getName()
            idStr = auth.getName();
        }

        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            // Agar username/subject UUID bo'lmasa, xabar aniq bo'lsin
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Current user id is not a valid UUID: " + idStr
            );
        }
    }

    /** Exception o'rniga null qaytaradigan variant (ixtiyoriy). */
    public static UUID currentUserIdOrNull() {
        try { return currentUserId(); } catch (Exception e) { return null; }
    }
}
