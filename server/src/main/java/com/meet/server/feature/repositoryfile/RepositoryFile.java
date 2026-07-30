package com.meet.server.feature.repositoryfile;

import com.meet.server.common.audit.BaseAuditEntity;
import com.meet.server.feature.codebase.Codebase;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "repository_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_repository_files_codebase_path",
                columnNames = {"codebase_id", "path"}
        ),
        indexes = @Index(name = "idx_repository_files_codebase", columnList = "codebase_id")
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "codebase")
public class RepositoryFile extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    private Codebase codebase;

    @Column(nullable = false)
    private String path;

    private String language;

    private String checksum;

    private Long size;
}
