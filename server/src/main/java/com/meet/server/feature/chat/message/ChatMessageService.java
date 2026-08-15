package com.meet.server.feature.chat.message;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.chat.dto.ChatHistoryResponse;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.chat.session.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 100;
    private static final String CURSOR_SEPARATOR = "|";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    public ChatHistoryResponse loadHistory(UUID sessionId, Integer requestedLimit, String rawCursor) {
        int limit = requestedLimit == null ? DEFAULT_HISTORY_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new CodebaseException(
                    "INVALID_CHAT_HISTORY_LIMIT",
                    "limit must be between 1 and " + MAX_HISTORY_LIMIT,
                    HttpStatus.BAD_REQUEST);
        }

        var cursor = rawCursor == null || rawCursor.isBlank() ? null : decodeCursor(rawCursor);
        var pageRequest = PageRequest.of(0, limit + 1);
        var messages = cursor == null
                ? chatMessageRepository.findRecentMessages(sessionId, pageRequest)
                : chatMessageRepository.findMessagesBefore(
                sessionId, cursor.createdAt(), cursor.messageId(), pageRequest);

        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = new ArrayList<>(messages.subList(0, limit));
        }

        String nextCursor = hasMore ? encodeCursor(messages.getLast()) : null;
        Collections.reverse(messages);
        return new ChatHistoryResponse(
                messages.stream().map(chatMessageMapper::toChatMessageResponse).toList(),
                hasMore,
                nextCursor);
    }

    public List<Message> loadPromptHistory(UUID sessionId) {
        var messages = new ArrayList<>(chatMessageRepository.findRecentMessages(sessionId, PageRequest.of(0, 20)));
        Collections.reverse(messages);
        return messages.stream().map(chatMessageMapper::toSpringMessage).toList();
    }

    public boolean hasAssistantResponse(UUID sessionId) {
        return chatMessageRepository.hasMessageWithRole(sessionId, MessageRole.ASSISTANT);
    }

    public void saveUserMessage(ChatSession session, String content) {
        saveMessage(session, MessageRole.USER, content, List.of());
    }

    public void saveAssistantMessage(ChatSession session, String content, List<CodeCitation> citations) {
        saveMessage(session, MessageRole.ASSISTANT, content, citations);
    }

    private void saveMessage(ChatSession session, MessageRole role, String content, List<CodeCitation> citations) {
        chatMessageRepository.save(ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .citations(citations == null ? List.of() : List.copyOf(citations))
                .build());
    }

    private String encodeCursor(ChatMessage message) {
        String value = message.getCreatedAt() + CURSOR_SEPARATOR + message.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String rawCursor) {
        try {
            String value = new String(
                    Base64.getUrlDecoder().decode(rawCursor), StandardCharsets.UTF_8);
            String[] parts = value.split(Pattern.quote(CURSOR_SEPARATOR), -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw new CodebaseException(
                    "INVALID_CHAT_HISTORY_CURSOR",
                    "before must be a valid chat history cursor",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private record Cursor(Instant createdAt, UUID messageId) {
    }
}
