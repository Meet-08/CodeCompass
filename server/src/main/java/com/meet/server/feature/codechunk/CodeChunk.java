package com.meet.server.feature.codechunk;

import com.meet.server.common.audit.BaseAuditEntity;
import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "code_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_code_chunks_file_chunk_index",
                columnNames = {"file_id", "chunk_index"}
        ),
        indexes = {
                @Index(name = "idx_code_chunks_codebase", columnList = "codebase_id"),
                @Index(name = "idx_code_chunks_file", columnList = "file_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"file", "codebase"})
public class CodeChunk extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private RepositoryFile file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    private Codebase codebase;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    private String language;

    @Column(nullable = false)
    private String path;

    private Integer startLine;

    private Integer endLine;

    private String commitSha;

}
