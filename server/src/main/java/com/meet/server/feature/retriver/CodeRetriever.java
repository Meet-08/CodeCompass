package com.meet.server.feature.retriver;

import com.meet.server.feature.codebase.dto.CodeCitation;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import com.meet.server.feature.embedding.SimilaritySearchRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodeRetriever {

    private static final Logger log = LogManager.getLogger(CodeRetriever.class);
    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingModel embeddingModel;

    public RetrievalContext retrieve(UUID codebaseId, String query) {
        log.debug("Retrieving code for {} using query {}", codebaseId, query);
        var request = SimilaritySearchRequest.builder()
                .codebaseId(codebaseId)
                .embedding(embeddingModel.embed(query))
                .topK(1)
                .build();
        var results = codeChunkRepository.similaritySearch(request);
        var citations = results.stream().map(result -> {
            var chunk = result.chunk();
            return new CodeCitation(chunk.getId(), chunk.getPath(), chunk.getStartLine(), chunk.getEndLine(), chunk.getLanguage(), result.distance());
        }).toList();

        StringBuilder context = new StringBuilder("\n\nRelevant code snippets:\n");
        int remaining = 12000;
        for (var result : results) {
            String content = result.chunk().getContent();
            if (content == null || content.isBlank() || remaining <= 0) continue;
            String snippet = content.substring(0, Math.min(content.length(), remaining));
            context.append("\n--- ").append(result.chunk().getPath()).append(":")
                    .append(result.chunk().getStartLine()).append("-").append(result.chunk().getEndLine())
                    .append(" ---\n").append(snippet).append('\n');
            remaining -= snippet.length();
        }
        log.debug("Total relevant code snippets: {}", citations.size());
        return new RetrievalContext(context.toString(), citations);
    }

    public record RetrievalContext(String promptContext, List<CodeCitation> citations) {
    }
}
