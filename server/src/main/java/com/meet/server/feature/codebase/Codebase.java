package com.meet.server.feature.codebase;

import com.meet.server.common.audit.BaseAuditEntity;
import com.meet.server.feature.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "codebases")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "user")
public class Codebase extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String cloneUrl;

    @Builder.Default
    private String branch = "main";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CodebaseStatus status = CodebaseStatus.QUEUED;

    private UUID lastCommitSha;

    private Instant indexedAt;
}
