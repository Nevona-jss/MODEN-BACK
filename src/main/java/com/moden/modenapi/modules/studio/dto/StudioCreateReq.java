package com.moden.modenapi.modules.studio.dto;

import jakarta.validation.constraints.*;

public record StudioCreateReq(
        @NotBlank @Size(max=150) String name,
        @Size(max=255) String qrCodeUrl,
        @Size(max=100) String businessNo,
        @Size(max=255) String address,
        @Size(max=50)  String phone
) {}