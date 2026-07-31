package com.meet.server.feature.codebase.dto;

import jakarta.validation.constraints.NotBlank;

public record CodebaseImportRequest(
        @NotBlank String name,
        @NotBlank String cloneUrl,
        String branch
) {
}
