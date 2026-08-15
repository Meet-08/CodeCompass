package com.meet.server.feature.codechunk;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.embedding.SimilaritySearchRequest;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import com.pgvector.PGhalfvec;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Repository
@RequiredArgsConstructor
public class CodeChunkRepositoryImpl implements CodeChunkRepository {

    private static final int BATCH_SIZE = 500;
    private static final int EMBEDDING_DIMENSIONS = 1024;

    private static final String CHUNK_COLUMNS = """
            c.id, c.created_at, c.updated_at, c.file_id, c.codebase_id, c.chunk_index, c.content,
            c.embedding, c.language, c.path, c.start_line, c.end_line,
            c.symbol_name, c.symbol_qualified_name, c.chunk_type, c.parent_symbol, c.commit_sha
            """;

    private static final String INSERT_SQL = """
            INSERT INTO code_chunks (
                id, created_at, updated_at, file_id, codebase_id, chunk_index, content, embedding,
                language, path, start_line, end_line,
                symbol_name, symbol_qualified_name, chunk_type, parent_symbol, commit_sha
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (file_id, chunk_index)
            DO UPDATE SET
                updated_at = EXCLUDED.updated_at,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                language = EXCLUDED.language,
                path = EXCLUDED.path,
                start_line = EXCLUDED.start_line,
                end_line = EXCLUDED.end_line,
                symbol_name = EXCLUDED.symbol_name,
                symbol_qualified_name = EXCLUDED.symbol_qualified_name,
                chunk_type = EXCLUDED.chunk_type,
                parent_symbol = EXCLUDED.parent_symbol,
                commit_sha = EXCLUDED.commit_sha
            """;

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    private static void bindChunk(
            PreparedStatement statement,
            CodeChunk chunk,
            Timestamp now
    ) throws SQLException {
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
        statement.setInt(6, chunk.getChunkIndex());
        statement.setString(7, chunk.getContent());
        setHalfVector(statement, 8, chunk.getEmbedding());
        statement.setString(9, chunk.getLanguage());
        statement.setString(10, chunk.getPath());
        setNullableInt(statement, 11, chunk.getStartLine());
        setNullableInt(statement, 12, chunk.getEndLine());
        statement.setString(13, chunk.getSymbolName());
        statement.setString(14, chunk.getSymbolQualifiedName());
        statement.setString(15, chunk.getChunkType() == null ? null : chunk.getChunkType().name());
        statement.setString(16, chunk.getParentSymbol());
        statement.setString(17, chunk.getCommitSha());
    }

    private static void setHalfVector(
            PreparedStatement statement,
            int index,
            float[] embedding
    ) throws SQLException {
        if (embedding == null) {
            statement.setNull(index, Types.OTHER);
            return;
        }

        validateEmbedding(embedding);

        statement.setObject(index, new PGhalfvec(embedding));
    }

    private static void validateEmbedding(float[] embedding) {
        if (embedding.length != EMBEDDING_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Expected a "
                            + EMBEDDING_DIMENSIONS
                            + "-dimensional embedding, but received "
                            + embedding.length
            );
        }
    }

    private static void setNullableInt(
            PreparedStatement statement,
            int index,
            Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static UUID requiredId(
            Object entity,
            String name
    ) {
        UUID id = entity instanceof RepositoryFile file
                ? file.getId()
                : entity instanceof Codebase codebase
                ? codebase.getId()
                : null;

        if (id == null)
            throw new IllegalArgumentException("A code chunk must have a " + name + " id");

        return id;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static float[] readHalfVector(
            Object value
    ) throws SQLException {
        switch (value) {
            case null -> {
                return null;
            }
            case PGhalfvec halfvec -> {
                float[] embedding = halfvec.toArray();
                validateEmbedding(embedding);
                return embedding;
            }
            case PGobject pgObject -> {
                String literal = pgObject.getValue();
                if (literal == null || literal.isBlank()) {
                    return null;
                }
                float[] embedding = parseHalfVectorLiteral(literal);
                validateEmbedding(embedding);
                return embedding;
            }
            case String literal -> {
                if (literal.isBlank()) {
                    return null;
                }
                float[] embedding = parseHalfVectorLiteral(literal);
                validateEmbedding(embedding);
                return embedding;
            }
            default -> {
            }
        }
        throw new SQLException(
                "Unexpected PostgreSQL embedding type: "
                        + value.getClass().getName()
        );
    }

    private static float[] parseHalfVectorLiteral(
            String literal
    ) throws SQLException {
        String value = literal.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new SQLException(
                    "Invalid halfvec value: " + literal
            );
        }

        String body = value.substring(1, value.length() - 1);

        if (body.isBlank()) {
            return new float[0];
        }

        String[] values = body.split(",");
        float[] embedding = new float[values.length];

        try {
            for (int i = 0; i < values.length; i++) {
                embedding[i] = Float.parseFloat(
                        values[i].trim()
                );
            }
        } catch (NumberFormatException ex) {
            throw new SQLException("Invalid halfvec value: " + literal, ex);
        }

        return embedding;
    }

    private static Instant readInstant(
            ResultSet rs,
            String column
    ) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null
                ? null
                : timestamp.toInstant();
    }

    private static String buildTsQuery(String query) {
        return Arrays.stream(query.trim().split("\\s+"))
                .map(token -> token.replaceAll("[^\\w]", ""))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.joining(" | "));
    }

    @Override
    public void saveAll(Collection<CodeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                chunks,
                BATCH_SIZE,
                (statement, chunk) ->
                        bindChunk(statement, chunk, now)
        );

        var chunksByFile = chunks.stream().collect(
                Collectors.groupingBy(
                        chunk ->
                                requiredId(chunk.getFile(), "file")
                )
        );

        for (var entry : chunksByFile.entrySet()) {
            var chunkIndexes = entry.getValue()
                    .stream()
                    .map(CodeChunk::getChunkIndex)
                    .toList();

            Map<Integer, UUID> persistedIds = new HashMap<>();

            jdbcClient.sql("""
                            SELECT id, chunk_index
                            FROM code_chunks
                            WHERE file_id = :fileId
                              AND chunk_index IN (:chunkIndexes)
                            """)
                    .param("fileId", entry.getKey())
                    .param("chunkIndexes", chunkIndexes)
                    .query((rs, rowNum) ->
                            Map.entry(
                                    rs.getInt("chunk_index"),
                                    rs.getObject("id", UUID.class)
                            )
                    )
                    .list()
                    .forEach(
                            id ->
                                    persistedIds.put(id.getKey(), id.getValue())
                    );

            for (CodeChunk chunk : entry.getValue()) {
                UUID persistedId =
                        persistedIds.get(chunk.getChunkIndex());

                if (persistedId == null) {
                    throw new IllegalStateException(
                            "Persisted code chunk was not found "
                                    + "for file="
                                    + entry.getKey()
                                    + ", chunkIndex="
                                    + chunk.getChunkIndex()
                    );
                }

                chunk.setId(persistedId);
            }
        }
    }

    @Override
    public void updateEmbedding(
            UUID chunkId,
            float[] embedding
    ) {
        if (embedding != null) {
            validateEmbedding(embedding);
        }

        jdbcClient.sql("""
                        UPDATE code_chunks
                        SET embedding = :embedding,
                            updated_at = :updatedAt
                        WHERE id = :id
                        """)
                .param("embedding",
                        embedding == null
                                ? null
                                : new PGhalfvec(embedding)
                )
                .param("updatedAt", Timestamp.from(Instant.now()))
                .param("id", chunkId)
                .update();
    }

    @Override
    public void deleteByFileId(UUID fileId) {
        jdbcClient.sql("""
                        DELETE FROM code_chunks
                        WHERE file_id = :fileId
                        """)
                .param("fileId", fileId)
                .update();
    }

    @Override
    public void deleteByCodebaseId(
            UUID codebaseId
    ) {
        jdbcClient.sql("""
                        DELETE FROM code_chunks
                        WHERE codebase_id = :codebaseId
                        """)
                .param("codebaseId", codebaseId)
                .update();
    }

    @Override
    public Optional<CodeChunk> findById(
            UUID chunkId
    ) {

        return jdbcClient.sql("""
                        SELECT %s
                        FROM code_chunks c
                        WHERE c.id = :id
                        """.formatted(
                        CHUNK_COLUMNS
                ))
                .param("id", chunkId)
                .query(this::mapChunk)
                .optional();
    }

    @Override
    public List<CodeChunk> findByCodebaseIdAndPath(
            UUID codebaseId,
            String path,
            Integer startLine,
            Integer endLine
    ) {
        if (codebaseId == null || !hasText(path)) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT %s
                FROM code_chunks c
                WHERE c.codebase_id = :codebaseId
                  AND c.path = :path
                """.formatted(CHUNK_COLUMNS));

        if (startLine != null) {
            sql.append("""
                      AND (c.end_line IS NULL OR c.end_line >= :startLine)
                    """);
        }
        if (endLine != null) {
            sql.append("""
                      AND (c.start_line IS NULL OR c.start_line <= :endLine)
                    """);
        }
        sql.append("""
                ORDER BY c.chunk_index
                """);

        var statement = jdbcClient.sql(sql.toString())
                .param("codebaseId", codebaseId)
                .param("path", path);
        if (startLine != null) {
            statement = statement.param("startLine", startLine);
        }
        if (endLine != null) {
            statement = statement.param("endLine", endLine);
        }
        return statement.query(this::mapChunk).list();
    }

    @Override
    public List<CodeChunk> findAroundChunk(
            UUID codebaseId,
            UUID chunkId,
            int radius
    ) {
        if (codebaseId == null || chunkId == null) {
            return List.of();
        }

        return jdbcClient.sql("""
                        SELECT %s
                        FROM code_chunks c
                        JOIN code_chunks origin
                          ON origin.id = :chunkId
                         AND origin.codebase_id = :codebaseId
                         AND c.codebase_id = origin.codebase_id
                         AND c.path = origin.path
                         AND c.chunk_index BETWEEN origin.chunk_index - :radius AND origin.chunk_index + :radius
                        ORDER BY c.chunk_index
                        """.formatted(CHUNK_COLUMNS))
                .param("codebaseId", codebaseId)
                .param("chunkId", chunkId)
                .param("radius", Math.max(0, radius))
                .query(this::mapChunk)
                .list();
    }

    @Override
    public List<SimilaritySearchResult> similaritySearch(
            SimilaritySearchRequest request
    ) {
        if (request == null
                || request.codebaseId() == null
                || request.embedding() == null
                || request.embedding().length == 0) {

            return List.of();
        }

        validateEmbedding(
                request.embedding()
        );

        StringBuilder sql = new StringBuilder("""
                SELECT ranked.*
                FROM (
                    SELECT
                        %s,
                        c.embedding <=> :embedding AS distance
                    FROM code_chunks c
                """.formatted(
                CHUNK_COLUMNS
        ));

        if (hasText(request.branch())) {
            sql.append("""
                    JOIN codebases b
                        ON b.id = c.codebase_id
                    """);
        }

        sql.append("""
                    WHERE c.codebase_id = :codebaseId
                      AND c.embedding IS NOT NULL
                """);

        if (hasText(request.language())) {
            sql.append("""
                    AND c.language = :language
                    """);
        }

        if (hasText(request.branch())) {
            sql.append("""
                    AND b.branch = :branch
                    """);
        }

        if (hasText(request.commitSha())) {
            sql.append("""
                    AND c.commit_sha = :commitSha
                    """);
        }

        sql.append("""
                ) ranked
                """);

        if (request.maxDistance() != null) {
            sql.append("""
                    WHERE ranked.distance <= :maxDistance
                    """);
        }

        sql.append("""
                ORDER BY ranked.distance
                LIMIT :topK
                """);

        var statement = jdbcClient
                .sql(sql.toString())
                .param("codebaseId", request.codebaseId())
                .param("embedding",
                        new PGhalfvec(
                                request.embedding()
                        )
                )
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

        return statement
                .query(this::mapSimilarityResult)
                .list();
    }

    private SimilaritySearchResult mapSimilarityResult(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new SimilaritySearchResult(
                mapChunk(rs, rowNum),
                rs.getDouble("distance")
        );
    }

    @Override
    public long countByCodebaseId(
            UUID codebaseId
    ) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM code_chunks
                        WHERE codebase_id = :codebaseId
                        """)
                .param("codebaseId", codebaseId)
                .query(Long.class)
                .single();
    }

    @Override
    public List<CodeChunk> findByCodebaseIdAndChunkType(
            UUID codebaseId,
            ChunkType chunkType
    ) {
        return jdbcClient
                .sql("""
                        SELECT %s
                        FROM code_chunks c
                        WHERE c.codebase_id = :codebaseId
                          AND c.chunk_type = :chunkType
                        """.formatted(
                        CHUNK_COLUMNS
                ))
                .param("codebaseId", codebaseId)
                .param("chunkType", chunkType.name())
                .query(this::mapChunk)
                .list();
    }

    @Override
    public List<FullTextSearchResult> fullTextSearch(
            UUID codebaseId,
            String query,
            int maxResults
    ) {
        if (codebaseId == null || !hasText(query)) {
            return List.of();
        }

        String tsQuery = buildTsQuery(query);
        if (tsQuery.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    %s,
                    ts_rank_cd(to_tsvector('simple', c.content), to_tsquery('simple', :tsQuery)) AS rank
                FROM code_chunks c
                WHERE c.codebase_id = :codebaseId
                  AND to_tsvector('simple', c.content) @@ to_tsquery('simple', :tsQuery)
                ORDER BY rank DESC
                LIMIT :maxResults
                """.formatted(CHUNK_COLUMNS);

        return jdbcClient.sql(sql)
                .param("codebaseId", codebaseId)
                .param("tsQuery", tsQuery)
                .param("maxResults", Math.max(1, maxResults))
                .query(this::mapFullTextSearchResult)
                .list();
    }

    private FullTextSearchResult mapFullTextSearchResult(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new FullTextSearchResult(
                mapChunk(rs, rowNum),
                rs.getDouble("rank")
        );
    }

    private CodeChunk mapChunk(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        String chunkTypeValue = rs.getString("chunk_type");
        CodeChunk chunk = CodeChunk.builder()
                .id(rs.getObject("id", UUID.class))
                .file(RepositoryFile.builder()
                        .id(rs.getObject("file_id", UUID.class))
                        .build()
                )
                .codebase(Codebase.builder()
                        .id(rs.getObject("codebase_id", UUID.class))
                        .build()
                )
                .chunkIndex(
                        (Integer) rs.getObject("chunk_index")
                )
                .content(rs.getString("content"))
                .embedding(readHalfVector(rs.getObject("embedding"))
                )
                .language(rs.getString("language"))
                .path(rs.getString("path"))
                .startLine((Integer) rs.getObject("start_line")
                )
                .endLine((Integer) rs.getObject("end_line")
                )
                .symbolName(rs.getString("symbol_name"))
                .symbolQualifiedName(rs.getString("symbol_qualified_name"))
                .chunkType(chunkTypeValue == null ? null : ChunkType.valueOf(chunkTypeValue))
                .parentSymbol(rs.getString("parent_symbol"))
                .commitSha(rs.getString("commit_sha")
                )
                .build();

        chunk.setCreatedAt(readInstant(rs, "created_at"));
        chunk.setUpdatedAt(readInstant(rs, "updated_at"));

        return chunk;
    }
}