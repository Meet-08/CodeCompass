package com.meet.server.feature.codebase;

import com.meet.server.common.api.ApiResponse;
import com.meet.server.feature.codebase.dto.CodebaseImportRequest;
import com.meet.server.feature.codebase.dto.CodebaseImportResponse;
import com.meet.server.feature.codebase.dto.CodeChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

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
        var response = codebaseService.startClone(
                UUID.fromString(authentication.getName()),
                request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(
                        true,
                        "Codebase import queued",
                        Optional.of(response)));
    }

    @PostMapping(value = "/{codebaseId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamChat(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @Valid @RequestBody CodeChatRequest request
    ) {
        return codebaseService.streamChat(UUID.fromString(authentication.getName()), codebaseId, request);
    }
}
