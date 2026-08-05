package com.meet.server.feature.codebase.dto;

import jakarta.validation.constraints.NotBlank;

public record CodebaseUpdateRequest(
        @NotBlank String name,
        @NotBlank String branch
) {}
