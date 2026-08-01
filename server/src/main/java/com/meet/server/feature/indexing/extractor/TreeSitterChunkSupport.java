package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.treesitter.TSNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

final class TreeSitterChunkSupport {

    static final int MAX_CHUNK_CHARACTERS = 4_000;
    static final int MAX_CONTEXT_CHARACTERS = 1_200;

    private TreeSitterChunkSupport() {
    }

    static String source(ParsedFile parsed, TSNode node) {
        return source(parsed, node.getStartByte(), node.getEndByte());
    }

    static String source(ParsedFile parsed, int startByte, int endByte) {
        byte[] bytes = parsed.content().getBytes(StandardCharsets.UTF_8);
        int start = Math.max(0, Math.min(startByte, bytes.length));
        int end = Math.max(start, Math.min(endByte, bytes.length));
        return new String(bytes, start, end - start, StandardCharsets.UTF_8);
    }

    static void addChunk(ParsedFile parsed, TSNode node, List<CodeChunk> chunks, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        chunks.add(CodeChunk.builder()
                .file(parsed.file())
                .codebase(parsed.file().getCodebase())
                .chunkIndex(chunks.size())
                .content(content.stripTrailing())
                .language(parsed.file().getLanguage())
                .path(parsed.file().getPath())
                .startLine(node.getStartPoint().getRow() + 1)
                .endLine(node.getEndPoint().getRow() + 1)
                .build());
    }

    static String boundedContext(String context) {
        if (context == null || context.isBlank()) {
            return "";
        }
        return context.substring(0, Math.min(context.length(), MAX_CONTEXT_CHARACTERS)).stripTrailing()
                + "\n\n";
    }
}
