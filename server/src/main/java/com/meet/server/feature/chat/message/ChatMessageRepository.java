package com.meet.server.feature.chat.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Query("select m from ChatMessage m where m.session.id = :sessionId order by m.createdAt desc")
    List<ChatMessage> findRecentMessages(@Param("sessionId") UUID sessionId, Pageable pageable);

    @Query("""
            select m from ChatMessage m
            where m.session.id = :sessionId
              and (m.createdAt < :createdAt
                   or (m.createdAt = :createdAt and m.id < :messageId))
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findMessagesBefore(
            @Param("sessionId") UUID sessionId,
            @Param("createdAt") Instant createdAt,
            @Param("messageId") UUID messageId,
            Pageable pageable
    );

    @Query("select count(m) > 0 from ChatMessage m where m.session.id = :sessionId and m.role = :role")
    boolean hasMessageWithRole(@Param("sessionId") UUID sessionId, @Param("role") MessageRole role);
}
