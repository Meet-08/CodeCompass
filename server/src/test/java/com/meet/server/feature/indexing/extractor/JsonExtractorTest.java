package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.indexing.parser.JsonParser;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JsonExtractorTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonParser parser = new JsonParser(jsonMapper);
    private final JsonExtractor extractor = new JsonExtractor(jsonMapper);

    @Test
    void emitsPathAwareChunksForNestedJson() {
        var chunks = extract("{\"name\":\"demo\",\"dependencies\":{\"spring\":\"4.1\"}}");

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().getContent().contains("JSON path: $"));
        assertTrue(chunks.getFirst().getContent().contains("dependencies"));
    }

    @Test
    void splitsLargeObjectsIntoNestedPathChunks() {
        var source = new StringBuilder("{\"dependencies\":{");
        for (int i = 0; i < 300; i++) {
            if (i > 0) source.append(',');
            source.append("\"dependency").append(i).append("\":\"").append("x".repeat(20)).append("\"");
        }
        source.append("}}");

        var chunks = extract(source.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= 4_000));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("$.dependencies.")));
        assertEquals(0, chunks.getFirst().getChunkIndex());
    }

    @Test
    void malformedJsonFallsBackToTextLanguage() {
        var parsed = parser.parse(file(), "{ malformed");

        assertEquals(com.meet.server.feature.indexing.language.Language.UNKNOWN, parsed.language());
        assertNull(parsed.jsonTree());
    }

    private java.util.List<com.meet.server.feature.codechunk.CodeChunk> extract(String content) {
        return extractor.extract(parser.parse(file(), content));
    }

    private RepositoryFile file() {
        var codebase = Codebase.builder().id(UUID.randomUUID()).name("test").build();
        return RepositoryFile.builder()
                .id(UUID.randomUUID())
                .codebase(codebase)
                .path("package.json")
                .language("json")
                .build();
    }
}
