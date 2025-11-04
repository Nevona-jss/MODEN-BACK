package com.moden.modenapi.common.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    public DecodedFirebaseUser verifyIdToken(String idToken) {
        try {
            // true => revoked (blok) qilingan tokenlarni ham tekshiradi
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);

            Map<String, Object> claims = token.getClaims();
            String phone = claims != null ? (String) claims.get("phone_number") : null;

            return new DecodedFirebaseUser(
                    token.getUid(),
                    phone,                    // <-- shu yerda olinyapti
                    token.getEmail(),
                    token.getName(),
                    token.getIssuer()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Firebase ID token: " + e.getMessage(), e);
        }
    }

    public record DecodedFirebaseUser(
            String uid,
            String phone,  // "+998...", "+82..." va h.k.
            String email,
            String name,
            String issuer
    ) {}
}
