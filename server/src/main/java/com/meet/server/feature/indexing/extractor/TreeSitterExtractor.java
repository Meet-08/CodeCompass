package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.ChunkType;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;
import org.treesitter.TSNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class TreeSitterExtractor implements ChunkExtractor {

    private static final int MAX_CHUNK_CHARACTERS = 4_000;
    private static final int MAX_CONTEXT_CHARACTERS = 1_200;

    private static final Set<String> METHOD_NODE_TYPES = Set.of(
            "method_declaration", "function_declaration", "function_definition",
            "function_item", "arrow_function", "method_definition"
    );
    private static final Set<String> CONSTRUCTOR_NODE_TYPES = Set.of(
            "constructor_declaration", "compact_constructor_declaration"
    );
    private static final Set<String> FIELD_NODE_TYPES = Set.of(
            "field_declaration", "field_definition", "class_field",
            "property_declaration", "lexical_declaration", "variable_declaration",
            "const_item"
    );
    private static final Set<String> CLASS_NODE_TYPES = Set.of(
            "class_declaration", "record_declaration", "struct_declaration",
            "struct_item", "object_declaration"
    );
    private static final Set<String> INTERFACE_NODE_TYPES = Set.of(
            "interface_declaration", "trait_item"
    );
    private static final Set<String> ENUM_NODE_TYPES = Set.of(
            "enum_declaration", "enum_item"
    );
    private static final Set<String> FUNCTION_NODE_TYPES = Set.of(
            "function_declaration", "function_definition", "function_item"
    );

    private static String buildQualifiedName(String parent, String name) {
        if (name == null) return parent;
        if (parent == null || parent.isEmpty()) return name;
        return parent + "." + name;
    }

    private static ChunkType resolveTypeChunkType(String nodeType) {
        if (INTERFACE_NODE_TYPES.contains(nodeType)) return ChunkType.INTERFACE;
        if (ENUM_NODE_TYPES.contains(nodeType)) return ChunkType.ENUM;
        return ChunkType.CLASS;
    }

    @Override
    public boolean supports(Language language) {
        return language.isProgramming();
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        if (parsed.rootNode() == null) {
            return List.of();
        }

        var chunks = new ArrayList<CodeChunk>();
        for (int i = 0; i < parsed.rootNode().getNamedChildCount(); i++) {
            emitTopLevel(parsed, parsed.rootNode().getNamedChild(i), chunks);
        }
        return chunks;
    }

    private void emitTopLevel(ParsedFile parsed, TSNode node, List<CodeChunk> chunks) {
        if (parsed.language().isIgnoredNode(node.getType())) {
            return;
        }

        if (sourceLength(parsed, node) <= MAX_CHUNK_CHARACTERS) {
            String symbolName = nodeName(parsed, node);
            ChunkType chunkType = resolveChunkType(parsed, node, null);
            addChunk(parsed, node, chunks, source(parsed, node),
                    chunkType, symbolName, symbolName, null);
            return;
        }

        if (parsed.language().isTypeDeclaration(node.getType())) {
            emitLargeType(parsed, node, chunks, null);
        } else {
            String symbolName = nodeName(parsed, node);
            ChunkType chunkType = resolveChunkType(parsed, node, null);
            emitLineChunks(parsed, node, chunks, "",
                    chunkType, symbolName, symbolName, null);
        }
    }

    private void emitLargeType(ParsedFile parsed, TSNode type, List<CodeChunk> chunks,
                               String outerParent) {
        TSNode body = type.getChildByFieldName("body");
        if (body == null || body.isNull() || body.getNamedChildCount() == 0) {
            String symbolName = nodeName(parsed, type);
            ChunkType chunkType = resolveTypeChunkType(type.getType());
            String qualifiedName = buildQualifiedName(outerParent, symbolName);
            emitLineChunks(parsed, type, chunks, "",
                    chunkType, symbolName, qualifiedName, outerParent);
            return;
        }

        String typeName = nodeName(parsed, type);
        String qualifiedTypeName = buildQualifiedName(outerParent, typeName);
        String context = typeContext(parsed, type, body);
        int before = chunks.size();
        for (int i = 0; i < body.getNamedChildCount(); i++) {
            TSNode member = body.getNamedChild(i);
            if (parsed.language().isIgnoredNode(member.getType())
                    || !parsed.language().isMemberDeclaration(member.getType())) {
                continue;
            }
            emitMember(parsed, member, context, chunks, qualifiedTypeName);
        }

        if (chunks.size() == before) {
            ChunkType chunkType = resolveTypeChunkType(type.getType());
            emitLineChunks(parsed, type, chunks, context,
                    chunkType, typeName, qualifiedTypeName, outerParent);
        }
    }

    private void emitMember(ParsedFile parsed, TSNode member, String context,
                            List<CodeChunk> chunks, String parentQualifiedName) {
        String content = context + source(parsed, member);
        String memberName = nodeName(parsed, member);
        ChunkType chunkType = resolveChunkType(parsed, member, parentQualifiedName);
        String qualifiedName = buildQualifiedName(parentQualifiedName, memberName);

        if (content.length() <= MAX_CHUNK_CHARACTERS) {
            addChunk(parsed, member, chunks, content,
                    chunkType, memberName, qualifiedName, parentQualifiedName);
        } else if (parsed.language().isTypeDeclaration(member.getType())) {
            emitLargeType(parsed, member, chunks, parentQualifiedName);
        } else {
            emitLineChunks(parsed, member, chunks, context,
                    chunkType, memberName, qualifiedName, parentQualifiedName);
        }
    }

    private String typeContext(ParsedFile parsed, TSNode type, TSNode body) {
        String signature = source(parsed, type.getStartByte(), body.getStartByte()).strip();
        var fields = new StringBuilder();
        for (int i = 0; i < body.getNamedChildCount(); i++) {
            TSNode child = body.getNamedChild(i);
            if (!parsed.language().isFieldDeclaration(child.getType())) {
                continue;
            }
            String field = source(parsed, child).strip();
            if (fields.length() + field.length() + 1 > MAX_CONTEXT_CHARACTERS / 2) {
                break;
            }
            fields.append(field).append('\n');
        }

        String context = "Class context:\n" + signature;
        if (!fields.isEmpty()) {
            context += "\nFields:\n" + fields.toString().stripTrailing();
        }
        return context.substring(0, Math.min(context.length(), MAX_CONTEXT_CHARACTERS))
                + "\n\nMember:\n";
    }

    private void emitLineChunks(ParsedFile parsed, TSNode node, List<CodeChunk> chunks,
                                String context, ChunkType chunkType, String symbolName,
                                String qualifiedName, String parentSymbol) {
        String[] lines = source(parsed, node).split("\\R", -1);
        StringBuilder current = new StringBuilder(context);
        int chunkStartLine = node.getStartPoint().getRow() + 1;
        int currentStartLine = chunkStartLine;

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int requiredLength = current.length() + line.length() + (current.length() > context.length() ? 1 : 0);
            if (requiredLength > MAX_CHUNK_CHARACTERS && current.length() > context.length()) {
                addChunk(parsed, node, chunks, current.toString().stripTrailing(),
                        currentStartLine, chunkStartLine + index - 1,
                        chunkType, symbolName, qualifiedName, parentSymbol);
                current = new StringBuilder(context);
                currentStartLine = chunkStartLine + index;
            }
            if (current.length() > context.length()) {
                current.append('\n');
            }
            current.append(line);
        }

        if (current.length() > context.length() && !current.toString().isBlank()) {
            addChunk(parsed, node, chunks, current.toString().stripTrailing(),
                    currentStartLine, node.getEndPoint().getRow() + 1,
                    chunkType, symbolName, qualifiedName, parentSymbol);
        }
    }

    private String nodeName(ParsedFile parsed, TSNode node) {
        TSNode nameNode = node.getChildByFieldName("name");
        if (nameNode == null || nameNode.isNull()) {
            return null;
        }
        String text = source(parsed, nameNode);
        return text.isBlank() ? null : text;
    }

    private ChunkType resolveChunkType(ParsedFile parsed, TSNode node, String parentContext) {
        String nodeType = node.getType();
        if (parsed.language().isTypeDeclaration(nodeType)) {
            return resolveTypeChunkType(nodeType);
        }
        if (CONSTRUCTOR_NODE_TYPES.contains(nodeType)) return ChunkType.CONSTRUCTOR;
        if (parentContext != null && METHOD_NODE_TYPES.contains(nodeType)) return ChunkType.METHOD;
        if (parentContext == null && FUNCTION_NODE_TYPES.contains(nodeType)) return ChunkType.FUNCTION;
        if (METHOD_NODE_TYPES.contains(nodeType)) return ChunkType.METHOD;
        if (FIELD_NODE_TYPES.contains(nodeType)) return ChunkType.FIELD;
        return null;
    }

    private int sourceLength(ParsedFile parsed, TSNode node) {
        return source(parsed, node).length();
    }

    private String source(ParsedFile parsed, TSNode node) {
        return source(parsed, node.getStartByte(), node.getEndByte());
    }

    private String source(ParsedFile parsed, int startByte, int endByte) {
        return TreeSitterChunkSupport.source(parsed, startByte, endByte);
    }

    private void addChunk(ParsedFile parsed, TSNode node, List<CodeChunk> chunks,
                          String content, ChunkType chunkType, String symbolName,
                          String qualifiedName, String parentSymbol) {
        addChunk(parsed, node, chunks, content,
                node.getStartPoint().getRow() + 1,
                node.getEndPoint().getRow() + 1,
                chunkType, symbolName, qualifiedName, parentSymbol);
    }

    private void addChunk(ParsedFile parsed, TSNode node, List<CodeChunk> chunks,
                          String content, int startLine, int endLine,
                          ChunkType chunkType, String symbolName,
                          String qualifiedName, String parentSymbol) {
        if (content == null || content.isBlank()) {
            return;
        }
        chunks.add(CodeChunk.builder()
                .file(parsed.file())
                .codebase(parsed.file().getCodebase())
                .chunkIndex(chunks.size())
                .content(content)
                .language(parsed.file().getLanguage())
                .path(parsed.file().getPath())
                .startLine(startLine)
                .endLine(endLine)
                .chunkType(chunkType)
                .symbolName(symbolName)
                .symbolQualifiedName(qualifiedName)
                .parentSymbol(parentSymbol)
                .build());
    }
}
