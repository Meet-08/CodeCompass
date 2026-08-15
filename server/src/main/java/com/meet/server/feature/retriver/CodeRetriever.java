package com.meet.server.feature.retriver;

import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import com.meet.server.feature.codechunk.FullTextSearchResult;
import com.meet.server.feature.codechunk.SimilaritySearchResult;
import com.meet.server.feature.embedding.SimilaritySearchRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CodeRetriever {

    private static final Logger log = LogManager.getLogger(CodeRetriever.class);
    private static final int CANDIDATES_PER_SIGNAL = 30;
    private static final int TOP_K_FINAL = 15;
    private static final int CONTEXT_CHAR_BUDGET = 16_000;

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingModel embeddingModel;

    public RetrievalContext retrieve(UUID codebaseId, String query) {
        log.info("Hybrid retrieval for codebase={} query=\"{}\"", codebaseId, query);

        var similarityFuture = CompletableFuture.supplyAsync(() -> {
            var request = SimilaritySearchRequest.builder()
                    .codebaseId(codebaseId)
                    .embedding(embeddingModel.embed(query))
                    .topK(CANDIDATES_PER_SIGNAL)
                    .build();
            return codeChunkRepository.similaritySearch(request);
        });

        var ftsFuture = CompletableFuture.supplyAsync(() ->
                codeChunkRepository.fullTextSearch(codebaseId, query, CANDIDATES_PER_SIGNAL)
        );

        List<SimilaritySearchResult> similarityResults;
        List<FullTextSearchResult> ftsResults;
        try {
            similarityResults = similarityFuture.join();
            ftsResults = ftsFuture.join();
        } catch (Exception e) {
            log.error("Parallel retrieval failed for codebase={}", codebaseId, e);
            return new RetrievalContext("", List.of());
        }

        log.debug("Candidates: {} similarity, {} full-text",
                similarityResults.size(), ftsResults.size());

        var similarityRanked = IntStream.range(0, similarityResults.size())
                .mapToObj(i -> new RrfReranker.RankedChunk(similarityResults.get(i).chunk(), i))
                .toList();

        var ftsRanked = IntStream.range(0, ftsResults.size())
                .mapToObj(i -> new RrfReranker.RankedChunk(ftsResults.get(i).chunk(), i))
                .toList();

        var fused = RrfReranker.fuse(similarityRanked, ftsRanked);
        var topChunks = fused.stream()
                .limit(TOP_K_FINAL)
                .toList();

        var citations = topChunks.stream()
                .map(sc -> new CodeCitation(
                        sc.chunk().getId(),
                        sc.chunk().getPath(),
                        sc.chunk().getStartLine(),
                        sc.chunk().getEndLine(),
                        sc.chunk().getLanguage(),
                        sc.score()))
                .toList();

        var context = new StringBuilder("\n\nRelevant code snippets:\n");
        int remaining = CONTEXT_CHAR_BUDGET;
        for (var sc : topChunks) {
            String content = sc.chunk().getContent();
            if (content == null || content.isBlank() || remaining <= 0) continue;
            String snippet = content.substring(0, Math.min(content.length(), remaining));
            context.append("\n--- ").append(sc.chunk().getPath()).append(":")
                    .append(sc.chunk().getStartLine()).append("-")
                    .append(sc.chunk().getEndLine())
                    .append(" (score=").append(String.format("%.4f", sc.score()))
                    .append(") ---\n").append(snippet).append('\n');
            remaining -= snippet.length();
        }

        log.info("Hybrid retrieval: {} similarity + {} FTS candidates → {} fused results",
                similarityResults.size(), ftsResults.size(), topChunks.size());

        return new RetrievalContext(context.toString(), citations);
    }

    public record RetrievalContext(String promptContext, List<CodeCitation> citations) {
    }
}
