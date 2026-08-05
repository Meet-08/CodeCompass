package com.meet.server.feature.codebase.mapper;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.codebase.dto.CodebaseResponse;
import org.springframework.stereotype.Component;

@Component
public class CodebaseMapper {

    public CodebaseResponse toCodebaseResponse(Codebase codebase, long fileCount) {
        return new CodebaseResponse(
                codebase.getId(),
                codebase.getName(),
                codebase.getCloneUrl(),
                codebase.getBranch(),
                codebase.getStatus(),
                codebase.getLastCommitSha(),
                codebase.getIndexedAt(),
                codebase.getCreatedAt(),
                codebase.getUpdatedAt(),
                fileCount);
    }
}
