package com.meet.server.feature.chat;

import com.meet.server.feature.chat.session.ChatSession;
import com.meet.server.feature.chat.session.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatTitleService {

    private final ChatSessionRepository chatSessionRepository;

    public String generateAndSave(ChatClient chatClient, ChatSession session, String prompt) {
        String title = session.getTitle();
        try {
            String generated = chatClient.prompt()
                    .user("Generate a concise chat title of 3 to 6 words for this prompt. "
                            + "Return only the title, without quotes or punctuation at the end:\n" + prompt)
                    .call()
                    .content();
            if (generated != null && !generated.isBlank()) {
                title = normalize(generated);
                session.setTitle(title);
                chatSessionRepository.save(session);
            }
        } catch (Exception exception) {
            log.error("Failed to generate chat title for sessionId={}, promptLength={}",
                    session.getId(), prompt.length(), exception);
        }
        return title;
    }

    private String normalize(String generated) {
        String title = generated.trim().replaceAll("[\\r\\n]+", " ");
        return title.length() > 255 ? title.substring(0, 255).trim() : title;
    }
}
