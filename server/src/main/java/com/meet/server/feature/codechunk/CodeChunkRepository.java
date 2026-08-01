package com.meet.server.feature.codechunk;

import com.meet.server.feature.codechunk.dto.SimilaritySearchRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeChunkRepository {

    void saveAll(Collection<CodeChunk> chunks);

    void updateEmbedding(UUID chunkId, float[] embedding);

    void deleteByFileId(UUID fileId);

    void deleteByCodebaseId(UUID codebaseId);

    Optional<CodeChunk> findById(UUID chunkId);

    List<SimilaritySearchResult> similaritySearch(SimilaritySearchRequest request);

    long countByCodebaseId(UUID codebaseId);
}
