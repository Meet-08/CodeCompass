package com.meet.server.feature.codebase.dto;

import com.meet.server.feature.codebase.CodebaseStatus;

import java.time.Instant;
import java.util.UUID;

public record CodebaseResponse(
        UUID codebaseId,
        String name,
        String cloneUrl,
        String branch,
        CodebaseStatus status,
        String lastCommitSha,
        Instant indexedAt,
        Instant createdAt,
        Instant updatedAt,
        long fileCount
) {}
