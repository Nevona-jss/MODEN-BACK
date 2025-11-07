package com.moden.modenapi.modules.designer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PortfolioAddReq(
        @NotEmpty
        List<@Size(min = 3, max = 500) String> paths, // universal uploads dan kelgan "path" (relPath)
        List<@Size(max = 255) String> captions       // ixtiyoriy, paths bilan uzunligi mos bo'lsa ishlatiladi
) {}
