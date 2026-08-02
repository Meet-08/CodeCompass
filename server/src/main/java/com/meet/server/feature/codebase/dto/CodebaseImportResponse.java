package com.meet.server.feature.codebase.dto;

import com.meet.server.feature.codebase.CodebaseStatus;

import java.util.UUID;

public record CodebaseImportResponse(
        UUID codebaseId,
        CodebaseStatus status,
        int fileCount
) {
}
