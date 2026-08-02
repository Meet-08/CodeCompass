package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.springframework.stereotype.Component;

@Component
public class TextParser implements Parser {
    @Override
    public boolean supports(Language language) {
        return language.parserKind() == Language.ParserKind.TEXT;
    }

    @Override
    public ParsedFile parse(RepositoryFile file, String content) {
        return new ParsedFile(file, Language.UNKNOWN, content, null, null, null);
    }
}
