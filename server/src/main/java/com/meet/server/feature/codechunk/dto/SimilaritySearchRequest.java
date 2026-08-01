package com.meet.server.feature.codechunk.dto;

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
