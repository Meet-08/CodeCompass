package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.indexing.parser.ParsedFile;
import com.meet.server.feature.indexing.parser.TreeSitterParser;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HtmlExtractorTest {

    private final TreeSitterParser parser = new TreeSitterParser();
    private final HtmlExtractor extractor = new HtmlExtractor();

    @Test
    void emitsHtmlElementsAsSourceAwareChunks() {
        var chunks = extract("""
                <!doctype html>
                <html>
                  <body>
                    <main class="content"><h1>Hello</h1><p>World</p></main>
                  </body>
                </html>
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("<main")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("<h1>Hello</h1>")));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getStartLine() != null && chunk.getEndLine() != null));
    }

    @Test
    void splitsLargeElementWithoutLosingOpeningTagContext() {
        var source = new StringBuilder("<section class=\"results\">\n");
        for (int i = 0; i < 300; i++) {
            source.append("  <article><h2>Result ").append(i).append("</h2><p>")
                    .append("x".repeat(20)).append("</p></article>\n");
        }
        source.append("</section>\n");

        var chunks = extract(source.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= 4_000));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("section class=\"results\"")));
    }

    @Test
    void recoversUsefulChunksFromMalformedTemplateMarkup() {
        var chunks = extract("<div th:if=\"${show\"><span>Visible</span>");

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("Visible")));
    }

    private java.util.List<com.meet.server.feature.codechunk.CodeChunk> extract(String content) {
        var codebase = Codebase.builder().id(UUID.randomUUID()).name("test").build();
        var file = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .codebase(codebase)
                .path("index.html")
                .language("html")
                .build();
        ParsedFile parsed = parser.parse(file, content);
        return extractor.extract(parsed);
    }
}
