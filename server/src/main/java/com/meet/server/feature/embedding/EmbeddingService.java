package com.meet.server.feature.embedding;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final int EMBEDDING_BATCH_SIZE = 32;

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingModel embeddingModel;

    public void embedChunks(List<CodeChunk> chunks) {
        for (int start = 0; start < chunks.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, chunks.size());
            var batch = chunks.subList(start, end);
            var embeddings = embeddingModel.embed(batch.stream()
                    .map(CodeChunk::getContent)
                    .toList());
            for (int index = 0; index < batch.size(); index++) {
                batch.get(index).setEmbedding(embeddings.get(index));
            }
        }

        codeChunkRepository.saveAll(chunks);
    }
}
