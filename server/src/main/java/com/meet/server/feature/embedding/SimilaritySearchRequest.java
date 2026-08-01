package com.meet.server.feature.embedding;

import java.util.UUID;

public record SimilaritySearchRequest(
        UUID codebaseId,
        float[] embedding,
        int topK,
        Double maxDistance,
        String language,
        String branch,
        String commitSha
) {
}
