package com.meet.server.feature.chat.session;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.chat.dto.ChatSessionResponse;
import com.meet.server.feature.chat.dto.ChatSessionUpdateRequest;
import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.codebase.CodebaseRepository;
import com.meet.server.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final CodebaseRepository codebaseRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final ChatSessionMapper chatSessionMapper;

    public ChatSession resolve(UUID userId, UUID codebaseId, String rawChatId) {
        Codebase codebase = ownedCodebase(userId, codebaseId);

        if (rawChatId != null && !rawChatId.isBlank()) {
            UUID sessionId;
            try {
                sessionId = UUID.fromString(rawChatId.trim());
            } catch (IllegalArgumentException exception) {
                throw new CodebaseException("INVALID_CHAT_ID", "chatId must be a valid UUID", HttpStatus.BAD_REQUEST);
            }

            return getChatSession(userId, codebaseId, sessionId);
        }

        var user = userRepository.findByIdForUpdate(userId).orElseThrow(() ->
                new CodebaseException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
        String title = nextUntitled(userId, codebaseId);
        return chatSessionRepository.save(ChatSession.builder()
                .user(user)
                .codebase(codebase)
                .title(title)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getUserCodebaseSessions(UUID userId, UUID codebaseId) {
        ownedCodebase(userId, codebaseId);
        return chatSessionRepository.findOwnedByUserAndCodebase(userId, codebaseId)
                .stream()
                .map(chatSessionMapper::toChatSessionResponse)
                .toList();
    }

    @Transactional
    public ChatSessionResponse updateTitle(
            UUID userId,
            UUID codebaseId,
            UUID sessionId,
            ChatSessionUpdateRequest request
    ) {
        ChatSession session = ownedSession(userId, codebaseId, sessionId);
        session.setTitle(request.title().trim());
        return chatSessionMapper.toChatSessionResponse(chatSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public ChatSession getOwnedSession(UUID userId, UUID codebaseId, UUID sessionId) {
        ownedCodebase(userId, codebaseId);
        return ownedSession(userId, codebaseId, sessionId);
    }

    @Transactional
    public void delete(UUID userId, UUID codebaseId, UUID sessionId) {
        chatSessionRepository.delete(ownedSession(userId, codebaseId, sessionId));
    }

    private Codebase ownedCodebase(UUID userId, UUID codebaseId) {
        Codebase codebase = codebaseRepository.findById(codebaseId).orElseThrow(() ->
                new CodebaseException("CODEBASE_NOT_FOUND", "Codebase not found", HttpStatus.NOT_FOUND));
        if (codebase.getUser() == null || !userId.equals(codebase.getUser().getId())) {
            throw new CodebaseException("CODEBASE_FORBIDDEN", "You do not own this codebase", HttpStatus.FORBIDDEN);
        }
        return codebase;
    }

    private ChatSession ownedSession(UUID userId, UUID codebaseId, UUID sessionId) {
        return getChatSession(userId, codebaseId, sessionId);
    }

    @NonNull
    private ChatSession getChatSession(UUID userId, UUID codebaseId, UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow(() ->
                new CodebaseException("CHAT_SESSION_NOT_FOUND", "Chat session not found", HttpStatus.NOT_FOUND));
        if (!userId.equals(session.getUser().getId()) || !codebaseId.equals(session.getCodebase().getId())) {
            throw new CodebaseException("CHAT_SESSION_FORBIDDEN", "You do not own this chat session", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    private String nextUntitled(UUID userId, UUID codebaseId) {
        int number = 1;
        while (chatSessionRepository.existsByOwnerAndCodebaseAndTitle(userId, codebaseId, "untitled-" + number)) {
            number++;
        }
        return "untitled-" + number;
    }
}
