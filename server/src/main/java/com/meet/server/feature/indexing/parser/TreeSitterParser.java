package com.meet.server.feature.indexing.parser;

import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.springframework.stereotype.Component;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TreeSitterParser implements Parser {

    private final Map<Language, TSLanguage> languages = new ConcurrentHashMap<>();

    @Override
    public boolean supports(Language language) {
        return language.parserKind() == Language.ParserKind.TREE_SITTER;
    }

    @Override
    public ParsedFile parse(RepositoryFile file, String content) {
        var language = Language.from(file.getLanguage());
        var grammar = languages.computeIfAbsent(language, this::loadGrammar);
        if (grammar == null) {
            throw new IllegalArgumentException("No Tree-sitter grammar configured for " + language);
        }

        var parser = new TSParser();
        if (!parser.setLanguage(grammar)) {
            throw new IllegalStateException("Unable to configure Tree-sitter grammar for " + language);
        }
        TSTree tree = parser.parseString(null, content);
        return new ParsedFile(file, language, content, tree, tree.getRootNode(), null);
    }

    public boolean supports(String language) {
        return Language.from(language).hasGrammar();
    }

    private TSLanguage loadGrammar(Language language) {
        var className = "org.treesitter." + language.grammarClass();
        try {
            Class<?> grammarClass = Class.forName(className);
            if (language.grammarMethod() != null) {
                Method method = grammarClass.getMethod(language.grammarMethod());
                return (TSLanguage) method.invoke(null);
            }
            return (TSLanguage) grammarClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Tree-sitter grammar is not available: " + className, exception);
        }
    }
}
