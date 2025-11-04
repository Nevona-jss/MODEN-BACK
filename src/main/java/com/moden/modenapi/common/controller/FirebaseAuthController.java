package com.moden.modenapi.common.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.service.FirebaseAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/firebase")
@RequiredArgsConstructor
public class FirebaseAuthController {

    private final FirebaseAuthService firebaseAuthService;

    public record VerifyReq(String idToken) {}

    @PostMapping("/verify")
    public ResponseEntity<ResponseMessage<FirebaseAuthService.DecodedFirebaseUser>> verify(@RequestBody VerifyReq req) {
        var user = firebaseAuthService.verifyIdToken(req.idToken());
        // shu yerda phone/email bo‘yicha sizning User tizimingizga bog‘lab JWT berishingiz mumkin
        return ResponseEntity.ok(ResponseMessage.success("Verified", user));
    }
}
