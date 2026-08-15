package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.ChunkType;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsonExtractor implements ChunkExtractor {

    private static final int MAX_CHUNK_CHARACTERS = 4_000;
    private final JsonMapper jsonMapper;

    public JsonExtractor(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(Language language) {
        return language == Language.JSON;
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        if (parsed.jsonTree() == null) {
            return List.of();
        }

        var chunks = new ArrayList<CodeChunk>();
        emit(parsed, parsed.jsonTree(), "$", chunks);
        return chunks;
    }

    private void emit(ParsedFile parsed, JsonNode node, String path, List<CodeChunk> chunks) {
        String serialized = jsonMapper.writeValueAsString(node);
        String content = "JSON path: " + path + "\n" + serialized;
        if (content.length() <= MAX_CHUNK_CHARACTERS || node.isValueNode()) {
            emitBounded(parsed, path, content, chunks);
            return;
        }

        if (node.isObject()) {
            for (var field : node.properties()) {
                emit(parsed, field.getValue(), path + "." + field.getKey(), chunks);
            }
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                emit(parsed, node.get(index), path + "[" + index + "]", chunks);
            }
        }
    }

    private void emitBounded(ParsedFile parsed, String path, String content, List<CodeChunk> chunks) {
        for (int start = 0; start < content.length(); start += MAX_CHUNK_CHARACTERS) {
            int end = Math.min(start + MAX_CHUNK_CHARACTERS, content.length());
            addChunk(parsed, path, content.substring(start, end), chunks);
        }
    }

    private void addChunk(ParsedFile parsed, String path, String content, List<CodeChunk> chunks) {
        chunks.add(CodeChunk.builder()
                .file(parsed.file())
                .codebase(parsed.file().getCodebase())
                .chunkIndex(chunks.size())
                .content(content)
                .language(parsed.file().getLanguage())
                .path(parsed.file().getPath())
                .startLine(null)
                .endLine(null)
                .chunkType(ChunkType.CONFIGURATION)
                .symbolQualifiedName(path)
                .build());
    }
}
