package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.common.enums.UserType;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.auth.dto.SignUpRequest;
import com.moden.modenapi.modules.auth.service.AuthService;
import com.moden.modenapi.modules.studio.dto.*;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class HairStudioController {

    private final HairStudioService service;
    private final AuthService authService;


    // 🔹 HAIR STUDIO REGISTER (super admin qo'shadi)
    @PostMapping("/register")
    public ResponseEntity<ResponseMessage<Void>> registerStudioAdmin(@RequestBody SignUpRequest req) {
        var fixedReq = new SignUpRequest(req.name(), req.phone(), UserType.HAIR_STUDIO);
        authService.signUp(fixedReq);
        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .success(true)
                        .message("Hair Studio successfully registered.")
                        .data(null)
                        .build()
        );
    }

    @GetMapping
    public List<StudioRes> list()
    { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<StudioRes>> get(@PathVariable UUID id) {
        var data = service.get(id);
        return ResponseEntity.ok(ResponseMessage.success("Studio retrieved successfully", data));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public StudioRes create(@RequestBody StudioCreateReq req)
    { return service.create(req); }
}
