package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.springframework.stereotype.Component;

/** Keeps Markdown parsing independent from native Tree-sitter grammars for now. */
@Component
public class MarkdownParser implements Parser {
    @Override
    public boolean supports(Language language) {
        return language.parserKind() == Language.ParserKind.MARKDOWN;
    }

    @Override
    public ParsedFile parse(RepositoryFile file, String content) {
        return new ParsedFile(file, Language.MARKDOWN, content, null, null, null);
    }
}
