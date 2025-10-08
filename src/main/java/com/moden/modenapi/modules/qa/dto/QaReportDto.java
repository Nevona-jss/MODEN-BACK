package com.moden.modenapi.modules.qa.dto;

import java.util.UUID;

public record QaReportDto(UUID id, String title, String testerName, String status, String summary) {}