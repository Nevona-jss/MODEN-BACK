package com.moden.modenapi.modules.auth.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.UserUpdateReq;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ UserController
 *
 * Handles profile-related operations for authenticated users.
 * Endpoint: PATCH /api/users/me
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 🔹 Update current user profile
     * Requires a valid JWT token in Authorization header.
     */
    @PatchMapping("/profile")
    public ResponseEntity<ResponseMessage<User>> updateUserProfile(
           @RequestBody UserUpdateReq req
    ) {
        User updatedUser = userService.updateProfile(req);

        return ResponseEntity.ok(
                ResponseMessage.<User>builder()
                        .success(true)
                        .message("Profile updated successfully")
                        .data(updatedUser)
                        .build()
        );
    }


    @GetMapping("/me")
    public ResponseEntity<ResponseMessage<User>> getCurrentUserProfile() {
        User user = userService.getCurrentUser();

        return ResponseEntity.ok(
                ResponseMessage.<User>builder()
                        .success(true)
                        .message("User profile fetched successfully")
                        .data(user)
                        .build()
        );
    }
}
