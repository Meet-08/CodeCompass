package com.meet.server.feature.indexing.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTest {

    @Test
    void mapsProgrammingAliasesToGrammarBackedLanguages() {
        for (var language : new Language[]{
                Language.JAVA, Language.KOTLIN, Language.PYTHON, Language.JAVASCRIPT,
                Language.TYPESCRIPT, Language.TSX, Language.GO, Language.RUST, Language.C, Language.CPP,
                Language.CSHARP, Language.PHP, Language.RUBY, Language.SWIFT
        }) {
            assertTrue(language.isProgramming());
            assertTrue(language.hasGrammar());
        }

        assertEquals(Language.TSX, Language.from("tsx"));
        assertEquals(Language.JAVASCRIPT, Language.from("jsx"));
        assertEquals(Language.CSHARP, Language.from(".cs"));
        assertEquals("java", Language.extensionOf("src/main/Demo.java"));
        assertEquals(Language.ParserKind.JSON, Language.JSON.parserKind());
        assertEquals(Language.ParserKind.MARKDOWN, Language.MARKDOWN.parserKind());
        assertEquals(Language.HTML, Language.from("html"));
        assertEquals(Language.HTML, Language.from(".htm"));
        assertEquals(Language.CSS, Language.from("css"));
        assertTrue(Language.HTML.hasGrammar());
        assertTrue(Language.CSS.hasGrammar());
        assertEquals(Language.ParserKind.TEXT, Language.UNKNOWN.parserKind());
    }
}
