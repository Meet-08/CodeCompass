package com.meet.server.feature.chat.tool;

import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import com.meet.server.feature.retriver.CodeRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeLookupToolsTest {

    private static final UUID CODEBASE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CHUNK_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private CodeChunkRepository codeChunkRepository;

    @Mock
    private CodeRetriever codeRetriever;

    private CodeLookupTools tools;

    @BeforeEach
    void setUp() {
        tools = new CodeLookupTools(codeChunkRepository, codeRetriever);
    }

    @Test
    void readMoreCodeRequiresCodebaseContext() {
        String result = tools.readMoreCode("src/Example.java", 1, 20, null, new ToolContext(Map.of()));

        assertEquals("Unable to read more code because the current codebase is unknown.", result);
        verifyNoInteractions(codeChunkRepository);
    }

    @Test
    void readMoreCodeRequiresPathOrChunkId() {
        String result = tools.readMoreCode(null, null, null, " ", toolContext(new ArrayList<>()));

        assertEquals("Provide a file path or chunkId to read more code.", result);
        verifyNoInteractions(codeChunkRepository);
    }

    @Test
    void readMoreCodeExpandsAroundChunkAndRecordsCitations() {
        var citations = new ArrayList<CodeCitation>();
        when(codeChunkRepository.findAroundChunk(CODEBASE_ID, CHUNK_ID, CodeLookupTools.DEFAULT_RADIUS))
                .thenReturn(List.of(chunk(CHUNK_ID, "src/Example.java", 10, 30, "    void index() {}")));

        String result = tools.readMoreCode(null, null, null, CHUNK_ID.toString(), toolContext(citations));

        assertTrue(result.contains("src/Example.java:10-30"));
        assertTrue(result.contains("void index() {}"));
        assertEquals(1, citations.size());
        assertEquals(CHUNK_ID, citations.getFirst().chunkId());
        assertEquals("src/Example.java", citations.getFirst().path());
    }

    @Test
    void readMoreCodeFallsBackToPathWhenChunkIsMissing() {
        when(codeChunkRepository.findAroundChunk(CODEBASE_ID, CHUNK_ID, CodeLookupTools.DEFAULT_RADIUS))
                .thenReturn(List.of());
        when(codeChunkRepository.findByCodebaseIdAndPath(CODEBASE_ID, "src/Example.java", 8, 40))
                .thenReturn(List.of(chunk(CHUNK_ID, "src/Example.java", 8, 40, "class Example {}")));

        String result = tools.readMoreCode("src/Example.java", 8, 40, CHUNK_ID.toString(), toolContext(new ArrayList<>()));

        assertTrue(result.contains("class Example {}"));
    }

    @Test
    void readMoreCodeReportsMissingLocation() {
        when(codeChunkRepository.findByCodebaseIdAndPath(CODEBASE_ID, "missing.java", null, null))
                .thenReturn(List.of());

        String result = tools.readMoreCode("missing.java", null, null, null, toolContext(new ArrayList<>()));

        assertEquals("No indexed code found for that location.", result);
    }

    @Test
    void searchCodeReturnsNoMatchWhenRetrievalIsEmpty() {
        when(codeRetriever.retrieve(CODEBASE_ID, "RepositoryFileProcessor"))
                .thenReturn(new CodeRetriever.RetrievalContext("", List.of()));

        String result = tools.searchCode("RepositoryFileProcessor", toolContext(new ArrayList<>()));

        assertEquals("No relevant indexed code found for that query.", result);
    }

    @Test
    void searchCodeRecordsRetrievedCitations() {
        var citations = new ArrayList<CodeCitation>();
        var citation = new CodeCitation(CHUNK_ID, "src/Example.java", 10, 30, "java", 0.4);
        when(codeRetriever.retrieve(CODEBASE_ID, "index files"))
                .thenReturn(new CodeRetriever.RetrievalContext("\n\nRelevant code snippets:\nclass Example {}", List.of(citation)));

        String result = tools.searchCode("index files", toolContext(citations));

        assertTrue(result.contains("class Example {}"));
        assertEquals(List.of(citation), citations);
    }

    private static ToolContext toolContext(List<CodeCitation> citations) {
        return new ToolContext(Map.of(
                CodeAdvisor.CODEBASE_ID_CONTEXT, CODEBASE_ID,
                CodeLookupTools.CITATIONS_SINK, citations
        ));
    }

    private static CodeChunk chunk(UUID id, String path, int startLine, int endLine, String content) {
        return CodeChunk.builder()
                .id(id)
                .path(path)
                .startLine(startLine)
                .endLine(endLine)
                .language("java")
                .content(content)
                .build();
    }
}
