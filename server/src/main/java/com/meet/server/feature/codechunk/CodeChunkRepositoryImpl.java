package com.meet.server.feature.codechunk;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.embedding.SimilaritySearchRequest;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CodeChunkRepositoryImpl implements CodeChunkRepository {

    private static final int BATCH_SIZE = 500;
    private static final String CHUNK_COLUMNS = """
            c.id, c.created_at, c.updated_at, c.file_id, c.codebase_id,
            c.chunk_index, c.content, c.embedding, c.language, c.path,
            c.start_line, c.end_line, c.commit_sha
            """;

    private static final String INSERT_SQL = """
            INSERT INTO code_chunks (
                id, created_at, updated_at, file_id, codebase_id, chunk_index,
                content, embedding, language, path, start_line, end_line, commit_sha
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (file_id, chunk_index) DO UPDATE SET
                updated_at = EXCLUDED.updated_at,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                language = EXCLUDED.language,
                path = EXCLUDED.path,
                start_line = EXCLUDED.start_line,
                end_line = EXCLUDED.end_line,
                commit_sha = EXCLUDED.commit_sha
            """;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    private static void bindChunk(PreparedStatement statement, CodeChunk chunk, Timestamp now)
            throws SQLException {
        UUID id = chunk.getId();
        if (id == null) {
            id = UUID.randomUUID();
            chunk.setId(id);
        }

        statement.setObject(1, id);
        statement.setTimestamp(2, now);
        statement.setTimestamp(3, now);
        statement.setObject(4, requiredId(chunk.getFile(), "file"));
        statement.setObject(5, requiredId(chunk.getCodebase(), "codebase"));
        statement.setObject(6, chunk.getChunkIndex());
        statement.setString(7, chunk.getContent());
        setVector(statement, 8, chunk.getEmbedding());
        statement.setString(9, chunk.getLanguage());
        statement.setString(10, chunk.getPath());
        setNullableInt(statement, 11, chunk.getStartLine());
        setNullableInt(statement, 12, chunk.getEndLine());
        statement.setString(13, chunk.getCommitSha());
    }

    private static void setVector(PreparedStatement statement, int index, float[] vector)
            throws SQLException {
        if (vector == null) {
            statement.setNull(index, Types.OTHER);
        } else {
            statement.setObject(index, new PGvector(vector));
        }
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static UUID requiredId(Object entity, String name) {
        UUID id = entity instanceof RepositoryFile file ? file.getId()
                : entity instanceof Codebase codebase ? codebase.getId() : null;
        if (id == null) {
            throw new IllegalArgumentException("A code chunk must have a " + name + " id");
        }
        return id;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static float[] readVector(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof PGvector vector) {
            return vector.toArray();
        }

        String literal = value instanceof PGobject pgObject ? pgObject.getValue() : value.toString();
        return new PGvector(literal).toArray();
    }

    private static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    @Override
    public void saveAll(Collection<CodeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate(INSERT_SQL, chunks, BATCH_SIZE,
                (statement, chunk) -> bindChunk(statement, chunk, now));
        var chunksByFile = chunks.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        chunk -> requiredId(chunk.getFile(), "file")));
        for (var entry : chunksByFile.entrySet()) {
            var chunkIndexes = entry.getValue().stream()
                    .map(CodeChunk::getChunkIndex)
                    .toList();
            Map<Integer, UUID> persistedIds = new HashMap<>();
            jdbcClient.sql("""
                            SELECT id, chunk_index FROM code_chunks
                            WHERE file_id = :fileId AND chunk_index IN (:chunkIndexes)
                            """)
                    .param("fileId", entry.getKey())
                    .param("chunkIndexes", chunkIndexes)
                    .query((rs, rowNum) -> Map.entry(
                            rs.getInt("chunk_index"), rs.getObject("id", UUID.class)))
                    .list()
                    .forEach(id -> persistedIds.put(id.getKey(), id.getValue()));
            for (CodeChunk chunk : entry.getValue()) {
                UUID persistedId = persistedIds.get(chunk.getChunkIndex());
                if (persistedId == null) {
                    throw new IllegalStateException("Persisted code chunk was not found");
                }
                chunk.setId(persistedId);
            }
        }
    }

    @Override
    public void updateEmbedding(UUID chunkId, float[] embedding) {
        jdbcClient.sql("""
                        UPDATE code_chunks
                        SET embedding = :embedding, updated_at = :updatedAt
                        WHERE id = :id
                        """)
                .param("embedding", embedding == null ? null : new PGvector(embedding))
                .param("updatedAt", Timestamp.from(Instant.now()))
                .param("id", chunkId)
                .update();
    }

    @Override
    public void deleteByFileId(UUID fileId) {
        jdbcClient.sql("DELETE FROM code_chunks WHERE file_id = :fileId")
                .param("fileId", fileId)
                .update();
    }

    @Override
    public void deleteByCodebaseId(UUID codebaseId) {
        jdbcClient.sql("DELETE FROM code_chunks WHERE codebase_id = :codebaseId")
                .param("codebaseId", codebaseId)
                .update();
    }

    @Override
    public Optional<CodeChunk> findById(UUID chunkId) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM code_chunks c
                        WHERE c.id = :id
                        """.formatted(CHUNK_COLUMNS))
                .param("id", chunkId)
                .query(this::mapChunk)
                .optional();
    }

    @Override
    public List<SimilaritySearchResult> similaritySearch(SimilaritySearchRequest request) {
        if (request == null || request.codebaseId() == null
                || request.embedding() == null || request.embedding().length == 0) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT ranked.*
                FROM (
                    SELECT %s, c.embedding <=> :embedding AS distance
                    FROM code_chunks c
                """.formatted(CHUNK_COLUMNS));

        if (hasText(request.branch())) {
            sql.append(" JOIN codebases b ON b.id = c.codebase_id");
        }
        sql.append("""
                    WHERE c.codebase_id = :codebaseId
                      AND c.embedding IS NOT NULL
                """);
        if (hasText(request.language())) {
            sql.append(" AND c.language = :language");
        }
        if (hasText(request.branch())) {
            sql.append(" AND b.branch = :branch");
        }
        if (hasText(request.commitSha())) {
            sql.append(" AND c.commit_sha = :commitSha");
        }
        sql.append(") ranked\n");
        if (request.maxDistance() != null) {
            sql.append("WHERE ranked.distance <= :maxDistance\n");
        }
        sql.append("ORDER BY ranked.distance\nLIMIT :topK\n");

        var statement = jdbcClient.sql(sql.toString())
                .param("codebaseId", request.codebaseId())
                .param("embedding", new PGvector(request.embedding()))
                .param("topK", Math.max(1, request.topK()));
        if (request.maxDistance() != null) {
            statement = statement.param("maxDistance", request.maxDistance());
        }
        if (hasText(request.language())) {
            statement = statement.param("language", request.language());
        }
        if (hasText(request.branch())) {
            statement = statement.param("branch", request.branch());
        }
        if (hasText(request.commitSha())) {
            statement = statement.param("commitSha", request.commitSha());
        }

        return statement.query(this::mapSimilarityResult).list();
    }

    private SimilaritySearchResult mapSimilarityResult(ResultSet rs, int rowNum) throws SQLException {
        return new SimilaritySearchResult(mapChunk(rs, rowNum), rs.getDouble("distance"));
    }

    @Override
    public long countByCodebaseId(UUID codebaseId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM code_chunks WHERE codebase_id = :codebaseId")
                .param("codebaseId", codebaseId)
                .query(Long.class)
                .single();
    }

    private CodeChunk mapChunk(ResultSet rs, int rowNum) throws SQLException {
        CodeChunk chunk = CodeChunk.builder()
                .id(rs.getObject("id", UUID.class))
                .file(RepositoryFile.builder().id(rs.getObject("file_id", UUID.class)).build())
                .codebase(Codebase.builder().id(rs.getObject("codebase_id", UUID.class)).build())
                .chunkIndex((Integer) rs.getObject("chunk_index"))
                .content(rs.getString("content"))
                .embedding(readVector(rs.getObject("embedding")))
                .language(rs.getString("language"))
                .path(rs.getString("path"))
                .startLine((Integer) rs.getObject("start_line"))
                .endLine((Integer) rs.getObject("end_line"))
                .commitSha(rs.getString("commit_sha"))
                .build();
        chunk.setCreatedAt(readInstant(rs, "created_at"));
        chunk.setUpdatedAt(readInstant(rs, "updated_at"));
        return chunk;
    }
}
