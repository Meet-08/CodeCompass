package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;

public interface Parser {

    boolean supports(Language language);

    ParsedFile parse(
            RepositoryFile file,
            String content
    );
}
