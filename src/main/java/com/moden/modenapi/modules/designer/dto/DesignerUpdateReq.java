package com.moden.modenapi.modules.designer.dto;

public record DesignerUpdateReq(
        String bio,
        String portfolioUrl,
        String phone,
        String idForLogin,
        String position
) {}
