package com.meet.server.feature.indexing.language;

import lombok.Getter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum Language {
    JAVA(true, ParserKind.TREE_SITTER, "TreeSitterJava", null, new String[]{"java"}),
    KOTLIN(true, ParserKind.TREE_SITTER, "TreeSitterKotlin", null, new String[]{"kotlin", "kt", "kts"}),
    PYTHON(true, ParserKind.TREE_SITTER, "TreeSitterPython", null, new String[]{"python", "py"}),
    JAVASCRIPT(true, ParserKind.TREE_SITTER, "TreeSitterJavascript", null, new String[]{"javascript", "js", "jsx", "mjs", "cjs"}),
    TYPESCRIPT(true, ParserKind.TREE_SITTER, "TreeSitterTypescript", null, new String[]{"typescript", "ts"}),
    TSX(true, ParserKind.TREE_SITTER, "TreeSitterTsx", null, new String[]{"tsx"}),
    GO(true, ParserKind.TREE_SITTER, "TreeSitterGo", null, new String[]{"go"}),
    RUST(true, ParserKind.TREE_SITTER, "TreeSitterRust", null, new String[]{"rust", "rs"}),
    C(true, ParserKind.TREE_SITTER, "TreeSitterC", null, new String[]{"c"}),
    CPP(true, ParserKind.TREE_SITTER, "TreeSitterCpp", null, new String[]{"cpp", "c++", "cc", "cxx", "hpp", "hxx"}),
    CSHARP(true, ParserKind.TREE_SITTER, "TreeSitterCSharp", null, new String[]{"csharp", "c#", "cs"}),
    PHP(true, ParserKind.TREE_SITTER, "TreeSitterPhp", null, new String[]{"php"}),
    RUBY(true, ParserKind.TREE_SITTER, "TreeSitterRuby", null, new String[]{"ruby", "rb"}),
    SWIFT(true, ParserKind.TREE_SITTER, "TreeSitterSwift", null, new String[]{"swift"}),

    HTML(false, ParserKind.TREE_SITTER, "TreeSitterHtml", null, new String[]{"html", "htm"}),
    CSS(false, ParserKind.TREE_SITTER, "TreeSitterCss", null, new String[]{"css"}),

    SQL(false, ParserKind.TEXT, null, null, new String[]{"sql"}),
    JSON(false, ParserKind.JSON, null, null, new String[]{"json"}),
    YAML(false, ParserKind.TEXT, null, null, new String[]{"yaml", "yml"}),
    XML(false, ParserKind.TEXT, null, null, new String[]{"xml"}),
    MARKDOWN(false, ParserKind.MARKDOWN, null, null, new String[]{"markdown", "md"}),
    DOCKERFILE(false, ParserKind.TEXT, null, null, new String[]{"dockerfile"}),
    PROPERTIES(false, ParserKind.TEXT, null, null, new String[]{"properties", "props"}),

    UNKNOWN(false, ParserKind.TEXT, null, null, new String[0]);

    public enum ParserKind {
        TREE_SITTER, JSON, MARKDOWN, TEXT
    }

    private static final Set<String> IGNORED_NODES = Set.of(
            "comment", "package_declaration", "import_declaration", "import_statement",
            "using_directive", "preproc_include", "preproc_def", "namespace_import"
    );
    private static final Set<String> TYPE_DECLARATIONS = Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration",
            "annotation_type_declaration", "struct_declaration", "namespace_definition",
            "object_declaration", "trait_item", "impl_item", "mod_item"
    );
    private static final Set<String> MEMBER_DECLARATIONS = Set.of(
            "field_declaration", "method_declaration", "function_declaration", "function_definition",
            "constructor_declaration", "compact_constructor_declaration", "static_initializer",
            "initializer", "lexical_declaration", "variable_declaration", "property_declaration",
            "const_item", "function_item", "struct_item", "enum_item", "class_declaration",
            "interface_declaration", "enum_declaration", "record_declaration", "struct_declaration",
            "object_declaration", "trait_item", "impl_item"
    );
    private static final Set<String> FIELD_DECLARATIONS = Set.of(
            "field_declaration", "field_definition", "class_field", "property_declaration"
    );
    private static final Map<String, Language> BY_ALIAS = buildAliasMap();

    @Getter
    private final boolean programming;
    private final ParserKind parserKind;
    private final String grammarClass;
    private final String grammarMethod;
    private final Set<String> aliases;

    Language(boolean programming, ParserKind parserKind, String grammarClass, String grammarMethod, String[] aliases) {
        this.programming = programming;
        this.parserKind = parserKind;
        this.grammarClass = grammarClass;
        this.grammarMethod = grammarMethod;
        this.aliases = Set.of(aliases);
    }

    public static Language from(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        String normalized = value.toLowerCase(Locale.ROOT).strip();
        if (normalized.startsWith(".")) normalized = normalized.substring(1);
        return BY_ALIAS.getOrDefault(normalized, UNKNOWN);
    }

    public static String extensionOf(String path) {
        if (path == null || path.isBlank()) return null;
        String fileName = Path.of(path).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT)
                : fileName.toLowerCase(Locale.ROOT);
    }

    private static Map<String, Language> buildAliasMap() {
        var aliases = new HashMap<String, Language>();
        for (Language language : values()) {
            for (String alias : language.aliases) {
                aliases.put(alias, language);
            }
        }
        return Map.copyOf(aliases);
    }

    public String grammarClass() {
        return grammarClass;
    }

    public String grammarMethod() {
        return grammarMethod;
    }

    public ParserKind parserKind() {
        return parserKind;
    }

    public boolean hasGrammar() {
        return grammarClass != null;
    }

    public boolean isIgnoredNode(String nodeType) {
        return IGNORED_NODES.contains(nodeType);
    }

    public boolean isTypeDeclaration(String nodeType) {
        return TYPE_DECLARATIONS.contains(nodeType);
    }

    public boolean isMemberDeclaration(String nodeType) {
        return MEMBER_DECLARATIONS.contains(nodeType);
    }

    public boolean isFieldDeclaration(String nodeType) {
        return FIELD_DECLARATIONS.contains(nodeType);
    }
}
