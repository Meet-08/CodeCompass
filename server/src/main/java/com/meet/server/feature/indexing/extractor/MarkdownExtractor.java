package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownExtractor implements ChunkExtractor {
    @Override
    public boolean supports(Language language) {
        return language == Language.MARKDOWN;
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        var chunks = new ArrayList<CodeChunk>();
        var current = new StringBuilder();
        int startLine = 1;
        var lines = parsed.content().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("#") && !current.isEmpty()) {
                add(parsed, chunks, current, startLine, i);
                current.setLength(0);
                startLine = i + 1;
            }
            current.append(lines[i]).append('\n');
        }
        if (!current.toString().isBlank()) add(parsed, chunks, current, startLine, lines.length);
        return chunks;
    }

    private void add(ParsedFile parsed, List<CodeChunk> chunks, StringBuilder content, int start, int end) {
        chunks.add(CodeChunk.builder().file(parsed.file()).codebase(parsed.file().getCodebase())
                .chunkIndex(chunks.size()).content(content.toString().stripTrailing())
                .language("markdown").path(parsed.file().getPath()).startLine(start).endLine(end).build());
    }
}
