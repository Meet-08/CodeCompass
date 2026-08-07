package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Creates bounded line-based chunks for text files without a syntax grammar. */
@Component
public class TextExtractor implements ChunkExtractor {
    private static final int MAX_LINES_PER_CHUNK = 120;
    private static final int MAX_CHUNK_CHARACTERS = 4_000;

    @Override
    public boolean supports(Language language) {
        return language == Language.UNKNOWN;
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        var chunks = new ArrayList<CodeChunk>();
        var lines = parsed.content().split("\\R", -1);
        for (int start = 0; start < lines.length; start += MAX_LINES_PER_CHUNK) {
            int end = Math.min(start + MAX_LINES_PER_CHUNK, lines.length);
            var current = new StringBuilder();
            int currentStart = start;
            for (int index = start; index < end; index++) {
                String line = lines[index];
                if (line.length() > MAX_CHUNK_CHARACTERS) {
                    add(parsed, chunks, current, currentStart, index);
                    current.setLength(0);
                    for (int offset = 0; offset < line.length(); offset += MAX_CHUNK_CHARACTERS) {
                        int lineEnd = Math.min(offset + MAX_CHUNK_CHARACTERS, line.length());
                        add(parsed, chunks, new StringBuilder(line.substring(offset, lineEnd)), index, index + 1);
                    }
                    currentStart = index + 1;
                } else if (current.length() > 0
                        && current.length() + line.length() + 1 > MAX_CHUNK_CHARACTERS) {
                    add(parsed, chunks, current, currentStart, index);
                    current = new StringBuilder();
                    currentStart = index;
                    current.append(line);
                } else {
                    if (current.length() > 0) current.append('\n');
                    current.append(line);
                }
            }
            add(parsed, chunks, current, currentStart, end);
        }
        return chunks;
    }

    private void add(ParsedFile parsed, List<CodeChunk> chunks, StringBuilder content, int start, int end) {
        String value = content.toString().stripTrailing();
        if (!value.isBlank()) {
            chunks.add(CodeChunk.builder()
                    .file(parsed.file())
                    .codebase(parsed.file().getCodebase())
                    .chunkIndex(chunks.size())
                    .content(value)
                    .language(parsed.file().getLanguage())
                    .path(parsed.file().getPath())
                    .startLine(start + 1)
                    .endLine(end)
                    .build());
        }
    }
}
