package com.moden.modenapi.modules.studio.controller;

import com.moden.modenapi.modules.studio.dto.*;
import com.moden.modenapi.modules.studio.service.HairStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class HairStudioController {
    private final HairStudioService service;

    @GetMapping public List<StudioRes> list(){ return service.list(); }
    @GetMapping("/{id}") public StudioRes get(@PathVariable UUID id){ return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public StudioRes create(@RequestBody StudioCreateReq req){ return service.create(req); }
}
