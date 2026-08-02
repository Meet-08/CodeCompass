package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;
import org.treesitter.TSNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TreeSitterExtractor implements ChunkExtractor {

    private static final int MAX_CHUNK_CHARACTERS = 4_000;
    private static final int MAX_CONTEXT_CHARACTERS = 1_200;

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
            addChunk(parsed, node, chunks, source(parsed, node));
            return;
        }

        if (parsed.language().isTypeDeclaration(node.getType())) {
            emitLargeType(parsed, node, chunks);
        } else {
            emitLineChunks(parsed, node, chunks, "");
        }
    }

    private void emitLargeType(ParsedFile parsed, TSNode type, List<CodeChunk> chunks) {
        TSNode body = type.getChildByFieldName("body");
        if (body == null || body.isNull() || body.getNamedChildCount() == 0) {
            emitLineChunks(parsed, type, chunks, "");
            return;
        }

        String context = typeContext(parsed, type, body);
        int before = chunks.size();
        for (int i = 0; i < body.getNamedChildCount(); i++) {
            TSNode member = body.getNamedChild(i);
            if (parsed.language().isIgnoredNode(member.getType())
                    || !parsed.language().isMemberDeclaration(member.getType())) {
                continue;
            }
            emitMember(parsed, member, context, chunks);
        }

        if (chunks.size() == before) {
            emitLineChunks(parsed, type, chunks, context);
        }
    }

    private void emitMember(ParsedFile parsed, TSNode member, String context, List<CodeChunk> chunks) {
        String content = context + source(parsed, member);
        if (content.length() <= MAX_CHUNK_CHARACTERS) {
            addChunk(parsed, member, chunks, content);
        } else if (parsed.language().isTypeDeclaration(member.getType())) {
            emitLargeType(parsed, member, chunks);
        } else {
            emitLineChunks(parsed, member, chunks, context);
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

    private void emitLineChunks(ParsedFile parsed, TSNode node, List<CodeChunk> chunks, String context) {
        String[] lines = source(parsed, node).split("\\R", -1);
        StringBuilder current = new StringBuilder(context);
        int chunkStartLine = node.getStartPoint().getRow() + 1;
        int currentStartLine = chunkStartLine;

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int requiredLength = current.length() + line.length() + (current.length() > context.length() ? 1 : 0);
            if (requiredLength > MAX_CHUNK_CHARACTERS && current.length() > context.length()) {
                addChunk(parsed, node, chunks, current.toString().stripTrailing(),
                        currentStartLine, chunkStartLine + index - 1);
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
                    currentStartLine, node.getEndPoint().getRow() + 1);
        }
    }

    private int sourceLength(ParsedFile parsed, TSNode node) {
        return source(parsed, node).length();
    }

    private String source(ParsedFile parsed, TSNode node) {
        return source(parsed, node.getStartByte(), node.getEndByte());
    }

    private String source(ParsedFile parsed, int startByte, int endByte) {
        byte[] bytes = parsed.content().getBytes(StandardCharsets.UTF_8);
        int start = Math.max(0, Math.min(startByte, bytes.length));
        int end = Math.max(start, Math.min(endByte, bytes.length));
        return new String(bytes, start, end - start, StandardCharsets.UTF_8);
    }

    private void addChunk(ParsedFile parsed, TSNode node, List<CodeChunk> chunks, String content) {
        addChunk(parsed, node, chunks, content,
                node.getStartPoint().getRow() + 1,
                node.getEndPoint().getRow() + 1);
    }

    private void addChunk(ParsedFile parsed, TSNode node, List<CodeChunk> chunks,
                          String content, int startLine, int endLine) {
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
                .build());
    }
}
