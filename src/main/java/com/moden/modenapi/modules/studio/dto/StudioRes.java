package com.moden.modenapi.modules.studio.dto;

import java.util.UUID;

public record StudioRes(UUID id, String name, String qrCodeUrl, String businessNo, String address, String phone) {}