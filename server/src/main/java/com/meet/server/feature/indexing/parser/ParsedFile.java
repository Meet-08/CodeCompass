package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.treesitter.TSNode;
import org.treesitter.TSTree;
import tools.jackson.databind.JsonNode;


public record ParsedFile(
        RepositoryFile file,
        Language language,
        String content,
        TSTree syntaxTree,
        TSNode rootNode,
        JsonNode jsonTree
) {
    public boolean hasSyntaxErrors() {
        return rootNode != null && rootNode.hasError();
    }
}
