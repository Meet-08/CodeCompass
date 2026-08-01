package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Creates bounded line-based chunks for text files without a syntax grammar. */
@Component
public class TextExtractor implements ChunkExtractor {
    private static final int MAX_LINES_PER_CHUNK = 120;

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
            var content = String.join("\n", Arrays.asList(lines).subList(start, end)).stripTrailing();
            if (!content.isBlank()) {
                chunks.add(CodeChunk.builder()
                        .file(parsed.file())
                        .codebase(parsed.file().getCodebase())
                        .chunkIndex(chunks.size())
                        .content(content)
                        .language(parsed.file().getLanguage())
                        .path(parsed.file().getPath())
                        .startLine(start + 1)
                        .endLine(end)
                        .build());
            }
        }
        return chunks;
    }
}
