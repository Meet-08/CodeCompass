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
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID codebaseId;
        private float[] embedding;
        private int topK;
        private Double maxDistance;
        private String language;
        private String branch;
        private String commitSha;
        public Builder codebaseId(UUID value) { codebaseId = value; return this; }
        public Builder embedding(float[] value) { embedding = value; return this; }
        public Builder topK(int value) { topK = value; return this; }
        public Builder maxDistance(Double value) { maxDistance = value; return this; }
        public Builder language(String value) { language = value; return this; }
        public Builder branch(String value) { branch = value; return this; }
        public Builder commitSha(String value) { commitSha = value; return this; }
        public SimilaritySearchRequest build() {
            return new SimilaritySearchRequest(codebaseId, embedding, topK, maxDistance, language, branch, commitSha);
        }
    }
}
