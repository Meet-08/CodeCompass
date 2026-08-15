package com.meet.server.feature.retriver;

import com.meet.server.feature.codechunk.CodeChunk;

import java.util.*;
import java.util.stream.Collectors;

public final class RrfReranker {

    private static final double K = 60.0;

    private RrfReranker() {}

    public record RankedChunk(CodeChunk chunk, int rank) {}

    public record ScoredChunk(CodeChunk chunk, double score) {}

    @SafeVarargs
    public static List<ScoredChunk> fuse(List<RankedChunk>... rankedLists) {
        Map<UUID, Double> scores = new HashMap<>();
        Map<UUID, CodeChunk> chunks = new HashMap<>();

        for (List<RankedChunk> list : rankedLists) {
            for (RankedChunk rc : list) {
                UUID id = rc.chunk().getId();
                chunks.putIfAbsent(id, rc.chunk());
                scores.merge(id, 1.0 / (K + rc.rank()), Double::sum);
            }
        }

        return scores.entrySet().stream()
                .map(e -> new ScoredChunk(chunks.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .collect(Collectors.toList());
    }
}
