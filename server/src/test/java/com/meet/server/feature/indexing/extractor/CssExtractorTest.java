package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.indexing.parser.ParsedFile;
import com.meet.server.feature.indexing.parser.TreeSitterParser;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CssExtractorTest {

    private final TreeSitterParser parser = new TreeSitterParser();
    private final CssExtractor extractor = new CssExtractor();

    @Test
    void emitsRulesAndPreservesSelectorContext() {
        var chunks = extract("""
                @import url(\"fonts.css\");
                :root { --brand-color: #123456; }
                .card, .panel { color: var(--brand-color); display: grid; }
                @media (min-width: 800px) { .card { grid-template-columns: 1fr 2fr; } }
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains(".card")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("--brand-color")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("@media")));
    }

    @Test
    void splitsLargeRuleIntoBoundedDeclarationChunks() {
        var source = new StringBuilder(".large-component {\n");
        for (int i = 0; i < 400; i++) {
            source.append("  --custom-property-").append(i).append(": ")
                    .append("x".repeat(20)).append(";\n");
        }
        source.append("}\n");

        var chunks = extract(source.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= 4_000));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().contains(".large-component")));
    }

    @Test
    void recoversUsefulChunksFromMalformedCss() {
        var chunks = extract(".broken { color: red; .nested { display: block; }");

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("color")));
    }

    private java.util.List<com.meet.server.feature.codechunk.CodeChunk> extract(String content) {
        var codebase = Codebase.builder().id(UUID.randomUUID()).name("test").build();
        var file = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .codebase(codebase)
                .path("styles.css")
                .language("css")
                .build();
        ParsedFile parsed = parser.parse(file, content);
        return extractor.extract(parsed);
    }
}
