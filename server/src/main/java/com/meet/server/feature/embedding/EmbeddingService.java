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

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingModel embeddingModel;

    public void embedChunks(List<CodeChunk> chunks) {
        for (var chunk : chunks) {
            chunk.setEmbedding(embeddingModel.embed(chunk.getContent()));
        }

        codeChunkRepository.saveAll(chunks);
    }
}
