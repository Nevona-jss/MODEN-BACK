package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FirebaseAuthService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.auth.service.AuthLocalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Phone Auth", description = "Firebase Phone verification & password reset")
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneAuthController {

    private final UserRepository userRepo;
    private final AuthLocalService authLocal;
    private final FirebaseAuthService firebaseAuthService;

    // very simple in-memory reset token store (TTL 10 min)
    private static final Map<String, ResetToken> TOKENS = new ConcurrentHashMap<>();

    public record VerifyReq(@NotBlank String idToken) {}
    public record VerifyRes(String resetToken) {}
    public record ResetReq(@NotBlank String resetToken, @NotBlank String newPassword) {}

    static class ResetToken {
        final UUID userId;
        final Instant expiresAt;
        ResetToken(UUID userId, Instant expiresAt) { this.userId = userId; this.expiresAt = expiresAt; }
    }

    @Operation(
            summary = "Verify Firebase ID token (after client phone-SMS auth)",
            description = "Returns a short-lived resetToken for password reset."
    )
    @PostMapping("/verify")
    public ResponseEntity<ResponseMessage<VerifyRes>> verify(@RequestBody VerifyReq req) {
        // verify and decode token (checks revocation too, per service impl)
        FirebaseAuthService.DecodedFirebaseUser decoded = firebaseAuthService.verifyIdToken(req.idToken());

        // Phone comes from the 'phone_number' claim
        String phone = decoded.phone();
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No phone in Firebase token");
        }

        // Optional: normalize phone format here if your DB stores E.164 strictly
        // phone = PhoneNormalizer.normalize(phone);

        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found by phone"));

        String resetToken = UUID.randomUUID().toString();
        TOKENS.put(resetToken, new ResetToken(user.getId(), Instant.now().plusSeconds(600))); // 10 min

        return ResponseEntity.ok(ResponseMessage.success("Phone verified", new VerifyRes(resetToken)));
    }

    @Operation(summary = "Reset password using resetToken")
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseMessage<String>> reset(@RequestBody ResetReq req) {
        ResetToken rt = TOKENS.remove(req.resetToken());
        if (rt == null || rt.expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired resetToken");
        }

        authLocal.createOrUpdatePassword(rt.userId, req.newPassword());
        return ResponseEntity.ok(ResponseMessage.success("Password reset success", "OK"));
    }
}
