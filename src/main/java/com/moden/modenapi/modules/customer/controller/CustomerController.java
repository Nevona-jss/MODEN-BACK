package com.moden.modenapi.modules.customer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Customer APIs")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @Operation(summary = "Customer: update my profile")
    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ResponseMessage<CustomerDetail>> updateMyProfile(@Valid @RequestBody CustomerProfileUpdateReq req) {
        var out = service.updateOwnProfile(req);
        return ResponseEntity.ok(ResponseMessage.success("Profile updated", out));
    }

}
