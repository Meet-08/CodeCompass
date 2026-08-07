package com.meet.server.feature.chat;

import com.meet.server.common.api.ApiResponse;
import com.meet.server.feature.chat.dto.CodeChatRequest;
import com.meet.server.feature.chat.dto.ChatHistoryResponse;
import com.meet.server.feature.chat.dto.ChatSessionResponse;
import com.meet.server.feature.chat.dto.ChatSessionUpdateRequest;
import com.meet.server.feature.chat.message.ChatMessageService;
import com.meet.server.feature.chat.session.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/codebases")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/{codebaseId}/chat/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> listSessions(
            Authentication authentication,
            @PathVariable UUID codebaseId
    ) {
        var sessions = chatSessionService.getUserCodebaseSessions(
                UUID.fromString(authentication.getName()), codebaseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Chat sessions retrieved", Optional.of(sessions)));
    }

    @PatchMapping("/{codebaseId}/chat/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> updateSessionTitle(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatSessionUpdateRequest request
    ) {
        var session = chatSessionService.updateTitle(
                UUID.fromString(authentication.getName()), codebaseId, sessionId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Chat session updated", Optional.of(session)));
    }

    @GetMapping("/{codebaseId}/chat/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getHistory(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @PathVariable UUID sessionId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String before
    ) {
        chatSessionService.getOwnedSession(
                UUID.fromString(authentication.getName()), codebaseId, sessionId);
        var history = chatMessageService.loadHistory(sessionId, limit, before);
        return ResponseEntity.ok(new ApiResponse<>(true, "Chat history retrieved", Optional.of(history)));
    }

    @DeleteMapping("/{codebaseId}/chat/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @PathVariable UUID sessionId
    ) {
        chatSessionService.delete(UUID.fromString(authentication.getName()), codebaseId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{codebaseId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamChat(
            Authentication authentication,
            @PathVariable UUID codebaseId,
            @Valid @RequestBody CodeChatRequest request
    ) {
        return chatService.stream(UUID.fromString(authentication.getName()), codebaseId, request);
    }
}
