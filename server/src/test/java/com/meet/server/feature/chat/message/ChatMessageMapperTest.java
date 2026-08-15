package com.meet.server.feature.chat.message;

import com.meet.server.feature.chat.dto.CodeCitation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageMapperTest {

    private final ChatMessageMapper mapper = new ChatMessageMapper();

    @Test
    void toChatMessageResponseCopiesCitations() {
        var citation = new CodeCitation(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "src/main/java/Example.java",
                10,
                30,
                "java",
                0.0328);
        var createdAt = Instant.parse("2026-08-07T10:00:00Z");
        var message = message(MessageRole.ASSISTANT, "Indexing starts here.", List.of(citation), createdAt);

        var response = mapper.toChatMessageResponse(message);

        assertEquals(message.getId(), response.messageId());
        assertEquals(MessageRole.ASSISTANT, response.role());
        assertEquals("Indexing starts here.", response.content());
        assertEquals(List.of(citation), response.citations());
        assertEquals(createdAt, response.createdAt());
        assertEquals(createdAt, response.updatedAt());
    }

    @Test
    void toChatMessageResponseExposesEmptyCitationsForUserMessages() {
        var message = message(MessageRole.USER, "Where is indexing implemented?", List.of(), Instant.parse("2026-08-07T10:00:00Z"));

        var response = mapper.toChatMessageResponse(message);

        assertEquals(MessageRole.USER, response.role());
        assertTrue(response.citations().isEmpty());
    }

    @Test
    void toChatMessageResponseTreatsNullCitationsAsEmpty() {
        var message = message(MessageRole.USER, "Hello", null, Instant.parse("2026-08-07T10:00:00Z"));

        var response = mapper.toChatMessageResponse(message);

        assertTrue(response.citations().isEmpty());
    }

    @Test
    void toSpringMessageUsesContentOnly() {
        var citation = new CodeCitation(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "src/main/java/Example.java",
                10,
                30,
                "java",
                0.0328);
        var createdAt = Instant.parse("2026-08-07T10:00:00Z");

        var user = mapper.toSpringMessage(message(MessageRole.USER, "user content", List.of(citation), createdAt));
        var assistant = mapper.toSpringMessage(message(MessageRole.ASSISTANT, "assistant content", List.of(citation), createdAt));
        var system = mapper.toSpringMessage(message(MessageRole.SYSTEM, "system content", List.of(citation), createdAt));

        assertInstanceOf(UserMessage.class, user);
        assertInstanceOf(AssistantMessage.class, assistant);
        assertInstanceOf(SystemMessage.class, system);
        assertEquals("user content", user.getText());
        assertEquals("assistant content", assistant.getText());
        assertEquals("system content", system.getText());
        assertTrue(user.getText().indexOf("Example.java") < 0);
        assertTrue(assistant.getText().indexOf("Example.java") < 0);
        assertTrue(system.getText().indexOf("Example.java") < 0);
    }

    private ChatMessage message(MessageRole role, String content, List<CodeCitation> citations, Instant timestamp) {
        var message = ChatMessage.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .role(role)
                .content(content)
                .citations(citations)
                .build();
        message.setCreatedAt(timestamp);
        message.setUpdatedAt(timestamp);
        return message;
    }
}
