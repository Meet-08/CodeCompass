package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.repositoryfile.RepositoryFile;

public interface Parser {
    ParsedFile parse(
            RepositoryFile file,
            String content
    );
}
