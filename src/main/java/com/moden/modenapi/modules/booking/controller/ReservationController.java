package com.moden.modenapi.modules.booking.controller;

import com.moden.modenapi.modules.booking.dto.*;
import com.moden.modenapi.modules.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService service;

    @GetMapping public List<ReservationRes> list(){ return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReservationRes create(@RequestBody ReservationCreateReq req){ return service.create(req); }
}
