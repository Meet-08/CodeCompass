package com.meet.server.feature.chat.session;


import com.meet.server.common.audit.BaseAuditEntity;
import com.meet.server.feature.chat.message.ChatMessage;
import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "chat_sessions",
        indexes = {
                @Index(name = "idx_chat_session_user", columnList = "user_id"),
                @Index(name = "idx_chat_session_codebase", columnList = "codebase_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    private Codebase codebase;

    @Column(nullable = false)
    private String title;

    @OneToMany(
            mappedBy = "session"
    )
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
}
