package com.meet.server.feature.codebase;

import com.meet.server.common.api.ApiResponse;
import com.meet.server.feature.codebase.dto.CodebaseImportRequest;
import com.meet.server.feature.codebase.dto.CodebaseImportResponse;
import com.meet.server.feature.codebase.dto.CodebaseResponse;
import com.meet.server.feature.codebase.dto.CodebaseUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/codebases")
@RequiredArgsConstructor
public class CodebaseController {

    private final CodebaseService codebaseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CodebaseImportResponse>> importCodebase(
            Authentication authentication,
            @Valid @RequestBody CodebaseImportRequest request
    ) {
        var response = codebaseService.startClone(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, "Codebase import queued", Optional.of(response)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CodebaseResponse>>> list(Authentication authentication) {
        var response = codebaseService.getUserCodebases(UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(new ApiResponse<>(true, "Codebases retrieved", Optional.of(response)));
    }

    @PatchMapping("/{codebaseId}")
    public ResponseEntity<ApiResponse<CodebaseResponse>> update(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @Valid @RequestBody CodebaseUpdateRequest request
    ) {
        var response = codebaseService.updateCodebase(UUID.fromString(authentication.getName()), codebaseId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Codebase updated", Optional.of(response)));
    }

    @DeleteMapping("/{codebaseId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID codebaseId) {
        codebaseService.deleteCodebase(UUID.fromString(authentication.getName()), codebaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{codebaseId}/reindex")
    public ResponseEntity<ApiResponse<CodebaseImportResponse>> reindex(
            Authentication authentication,
            @PathVariable UUID codebaseId
    ) {
        var response = codebaseService.reindexCodebase(UUID.fromString(authentication.getName()), codebaseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, "Codebase reindex queued", Optional.of(response)));
    }
}
