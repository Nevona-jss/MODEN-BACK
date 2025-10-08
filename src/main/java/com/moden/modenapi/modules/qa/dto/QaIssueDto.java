package com.moden.modenapi.modules.qa.dto;

import java.util.UUID;

public record QaIssueDto(UUID id, UUID reportId, String type, String severity, String description) {}