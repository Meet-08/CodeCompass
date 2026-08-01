package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class JsonParser implements Parser {

    private final JsonMapper jsonMapper;

    public JsonParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(Language language) {
        return language.parserKind() == Language.ParserKind.JSON;
    }

    @Override
    public ParsedFile parse(RepositoryFile file, String content) {
        try {
            JsonNode jsonTree = jsonMapper.readTree(content);
            if (jsonTree == null) {
                log.warn("Unable to parse empty JSON file {}; using text extraction", file.getPath());
                return new ParsedFile(file, Language.UNKNOWN, content, null, null, null);
            }
            return new ParsedFile(file, Language.JSON, content, null, null, jsonTree);
        } catch (JacksonException exception) {
            log.warn("Unable to parse JSON file {}; using text extraction", file.getPath());
            return new ParsedFile(file, Language.UNKNOWN, content, null, null, null);
        }
    }
}
