package com.moden.modenapi.modules.customer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.consult.service.ConsultationService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.model.UserSession;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.customer.service.CustomerService;
import com.moden.modenapi.modules.auth.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService userService;
    private final UserSessionService userSessionService;
    private final HttpServletRequest request;
    private final CustomerService customerService;
    // ----------------------------------------------------------------------
    // 🔹 CUSTOMER SIGN-UP (Create User + Session)
    // ----------------------------------------------------------------------
    @Operation(
            summary = "Customer Sign-Up",
            description = "Creates a new user, session, and issues a first-visit coupon for the selected studio."
    )
    @PostMapping("/studios/signup")
    public ResponseEntity<ResponseMessage<User>> signUp(
            @Valid @RequestBody CustomerSignUpRequest req,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Device-Type", required = false) String deviceType,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        String ipAddress = request.getRemoteAddr();

        User created = customerService.createUser(
                req,
                deviceId != null ? deviceId : "unknown-device",
                deviceType != null ? deviceType : "unknown-type",
                ipAddress,
                userAgent,
                appVersion
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Customer account created successfully", created));
    }
    @GetMapping("/customers/me")
    public ResponseEntity<ResponseMessage<User>> getCurrentUserProfile() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(ResponseMessage.success("User profile fetched successfully", user));
    }

    @PatchMapping("/customers/update")
    public ResponseEntity<ResponseMessage<?>> updateProfile(@Valid @RequestBody CustomerProfileUpdateReq req) {
        User currentUser = userService.getCurrentUser();
        var updatedDetail = userService.updateUserDetail(currentUser.getId(), req);
        return ResponseEntity.ok(ResponseMessage.success("Profile updated successfully", updatedDetail));
    }

    @GetMapping("/customers/sessions")
    public ResponseEntity<ResponseMessage<List<UserSession>>> getActiveSessions() {
        User currentUser = userService.getCurrentUser();
        List<UserSession> sessions = userSessionService.getActiveSessions(currentUser.getId());
        return ResponseEntity.ok(ResponseMessage.success("Active sessions retrieved successfully", sessions));
    }

    @DeleteMapping("/customers/delete")
    public ResponseEntity<ResponseMessage<Void>> deleteAccount() {
        User currentUser = userService.getCurrentUser();
        userService.deleteUser(currentUser.getId());

        ResponseCookie expired = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(ResponseMessage.success("Account deleted successfully", null));
    }
}
