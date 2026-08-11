package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.ChunkType;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownExtractor implements ChunkExtractor {
    private static final int MAX_CHUNK_CHARACTERS = 4_000;

    @Override
    public boolean supports(Language language) {
        return language == Language.MARKDOWN;
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        var chunks = new ArrayList<CodeChunk>();
        var current = new StringBuilder();
        int currentStartLine = 1;
        boolean inFence = false;
        var lines = parsed.content().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!inFence && line.startsWith("#") && !current.isEmpty()) {
                add(parsed, chunks, current, currentStartLine, i);
                current.setLength(0);
                currentStartLine = i + 1;
            }

            if (current.length() > 0 && current.length() + line.length() + 1 > MAX_CHUNK_CHARACTERS) {
                add(parsed, chunks, current, currentStartLine, i);
                current.setLength(0);
                currentStartLine = i + 1;
            }

            if (line.length() > MAX_CHUNK_CHARACTERS) {
                for (int offset = 0; offset < line.length(); offset += MAX_CHUNK_CHARACTERS) {
                    int end = Math.min(offset + MAX_CHUNK_CHARACTERS, line.length());
                    add(parsed, chunks, new StringBuilder(line.substring(offset, end)),
                            i + 1, i + 1);
                }
                currentStartLine = i + 2;
                continue;
            }

            current.append(line).append('\n');
            if (line.stripLeading().startsWith("```")) {
                inFence = !inFence;
            }
        }
        if (!current.toString().isBlank()) add(parsed, chunks, current, currentStartLine, lines.length);
        return chunks;
    }

    private void add(ParsedFile parsed, List<CodeChunk> chunks, StringBuilder content, int start, int end) {
        String text = content.toString().stripTrailing();
        String symbolName = extractHeading(text);
        chunks.add(CodeChunk.builder().file(parsed.file()).codebase(parsed.file().getCodebase())
                .chunkIndex(chunks.size()).content(text)
                .language("markdown").path(parsed.file().getPath()).startLine(start).endLine(end)
                .chunkType(ChunkType.MARKDOWN_SECTION)
                .symbolName(symbolName)
                .build());
    }

    private static String extractHeading(String text) {
        if (text == null || text.isBlank()) return null;
        String firstLine = text.lines().findFirst().orElse("");
        if (firstLine.startsWith("#")) {
            return firstLine.replaceFirst("^#+\\s*", "").strip();
        }
        return null;
    }
}
