package com.meet.server.feature.user;

import com.meet.server.common.audit.BaseAuditEntity;
import com.meet.server.feature.auth.Provider;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username", unique = true),
                @Index(name = "idx_user_email", columnList = "email", unique = true)
        })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"password"})
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String fullName;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    private String avatarUrl;

    @Builder.Default
    private UserRole role = UserRole.USER;

    @Builder.Default
    private Provider provider = Provider.EMAIL;
}
