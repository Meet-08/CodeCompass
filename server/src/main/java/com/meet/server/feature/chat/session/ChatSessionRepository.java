package com.meet.server.feature.chat.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    @Query("select s from ChatSession s where s.user.id = :userId and s.codebase.id = :codebaseId order by s.updatedAt desc")
    List<ChatSession> findOwnedByUserAndCodebase(
            @Param("userId") UUID userId,
            @Param("codebaseId") UUID codebaseId
    );

    @Query("select count(s) > 0 from ChatSession s where s.user.id = :userId and s.codebase.id = :codebaseId and s.title = :title")
    boolean existsByOwnerAndCodebaseAndTitle(
            @Param("userId") UUID userId,
            @Param("codebaseId") UUID codebaseId,
            @Param("title") String title
    );
}
