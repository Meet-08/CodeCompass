package com.meet.server.feature.codebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodebaseImportRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "(?i)^https://.+$", message = "cloneUrl must use HTTPS")
        String cloneUrl,
        String branch
) {
}
