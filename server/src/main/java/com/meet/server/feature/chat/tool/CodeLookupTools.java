package com.meet.server.feature.chat.tool;

import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import com.meet.server.feature.retriver.CodeRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CodeLookupTools {

    public static final String CITATIONS_SINK = "toolCitations";

    static final int CHAR_BUDGET = 12_000;
    static final int DEFAULT_RADIUS = 2;

    private final CodeChunkRepository codeChunkRepository;
    private final CodeRetriever codeRetriever;

    @Tool(
            name = "read_more_code",
            description = "Fetch more indexed source when a retrieved snippet is truncated or missing surrounding context. Provide a chunkId to expand neighboring chunks, or a file path with an optional line range."
    )
    public String readMoreCode(
            @ToolParam(description = "Indexed file path from a retrieved snippet", required = false) String path,
            @ToolParam(description = "Optional 1-based start line", required = false) Integer startLine,
            @ToolParam(description = "Optional 1-based end line", required = false) Integer endLine,
            @ToolParam(description = "Optional citation chunkId to expand neighboring chunks", required = false) String chunkId,
            ToolContext toolContext
    ) {
        Optional<UUID> codebaseId = resolveCodebaseId(toolContext);
        if (codebaseId.isEmpty()) {
            return "Unable to read more code because the current codebase is unknown.";
        }

        List<CodeChunk> chunks = List.of();
        UUID parsedChunkId = parseUuid(chunkId);
        if (parsedChunkId != null) {
            chunks = codeChunkRepository.findAroundChunk(codebaseId.get(), parsedChunkId, DEFAULT_RADIUS);
        }
        if (chunks.isEmpty() && hasText(path)) {
            chunks = codeChunkRepository.findByCodebaseIdAndPath(codebaseId.get(), path.trim(), startLine, endLine);
        }
        if (chunks.isEmpty()) {
            if (!hasText(path) && parsedChunkId == null) {
                return "Provide a file path or chunkId to read more code.";
            }
            return "No indexed code found for that location.";
        }

        List<CodeCitation> citations = new ArrayList<>();
        String formatted = formatChunks("Additional code", chunks, citations);
        recordCitations(toolContext, citations);
        return formatted;
    }

    @Tool(
            name = "search_code",
            description = "Search the indexed codebase again with a more specific query when the retrieved snippets are incomplete or not relevant enough."
    )
    public String searchCode(
            @ToolParam(description = "More specific query such as a class, method, or distinctive term") String query,
            ToolContext toolContext
    ) {
        Optional<UUID> codebaseId = resolveCodebaseId(toolContext);
        if (codebaseId.isEmpty()) {
            return "Unable to search code because the current codebase is unknown.";
        }
        if (!hasText(query)) {
            return "Provide a more specific search query.";
        }

        var retrieval = codeRetriever.retrieve(codebaseId.get(), query.trim());
        if (retrieval.citations().isEmpty()) {
            return "No relevant indexed code found for that query.";
        }
        recordCitations(toolContext, retrieval.citations());
        return retrieval.promptContext();
    }

    private static Optional<UUID> resolveCodebaseId(ToolContext toolContext) {
        if (toolContext == null) {
            return Optional.empty();
        }
        Object raw = toolContext.getContext().get(CodeAdvisor.CODEBASE_ID_CONTEXT);
        if (raw instanceof UUID id) {
            return Optional.of(id);
        }
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parseUuid(raw.toString()));
    }

    private static UUID parseUuid(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String formatChunks(String heading, List<CodeChunk> chunks, List<CodeCitation> citations) {
        var context = new StringBuilder("\n\n").append(heading).append(":\n");
        int remaining = CHAR_BUDGET;
        for (var chunk : chunks) {
            String content = chunk.getContent();
            if (content == null || content.isBlank() || remaining <= 0) {
                continue;
            }
            String snippet = content.substring(0, Math.min(content.length(), remaining));
            context.append("\n--- ").append(chunk.getPath()).append(":")
                    .append(chunk.getStartLine()).append("-")
                    .append(chunk.getEndLine())
                    .append(" ---\n")
                    .append(snippet)
                    .append('\n');
            remaining -= snippet.length();
            citations.add(new CodeCitation(
                    chunk.getId(),
                    chunk.getPath(),
                    chunk.getStartLine(),
                    chunk.getEndLine(),
                    chunk.getLanguage(),
                    0.0
            ));
        }
        if (citations.isEmpty()) {
            return "No indexed code found for that location.";
        }
        return context.toString();
    }

    @SuppressWarnings("unchecked")
    private static void recordCitations(ToolContext toolContext, List<CodeCitation> found) {
        if (toolContext == null || found == null || found.isEmpty()) {
            return;
        }
        Object sink = toolContext.getContext().get(CITATIONS_SINK);
        if (sink instanceof List<?> list) {
            ((List<Object>) list).addAll(found);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
