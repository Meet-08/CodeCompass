package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;
import org.treesitter.TSNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class HtmlExtractor implements ChunkExtractor {

    @Override
    public boolean supports(Language language) {
        return language == Language.HTML;
    }

    @Override
    public List<CodeChunk> extract(ParsedFile parsed) {
        if (parsed.rootNode() == null) {
            return List.of();
        }
        var chunks = new ArrayList<CodeChunk>();
        for (int i = 0; i < parsed.rootNode().getNamedChildCount(); i++) {
            emit(parsed, parsed.rootNode().getNamedChild(i), "", chunks);
        }
        return chunks;
    }

    private void emit(ParsedFile parsed, TSNode node, String inheritedContext, List<CodeChunk> chunks) {
        String content = TreeSitterChunkSupport.source(parsed, node);
        if (content.isBlank() || "comment".equals(node.getType())) {
            return;
        }
        String complete = inheritedContext + content;
        if (complete.length() <= TreeSitterChunkSupport.MAX_CHUNK_CHARACTERS) {
            TreeSitterChunkSupport.addChunk(parsed, node, chunks, complete);
            return;
        }

        String context = TreeSitterChunkSupport.boundedContext(inheritedContext + openingContext(parsed, node));
        int before = chunks.size();
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            TSNode child = node.getNamedChild(i);
            if (isStructuralWrapper(child) || "comment".equals(child.getType())) {
                continue;
            }
            emit(parsed, child, context, chunks);
        }
        if (chunks.size() == before) {
            emitLineChunks(parsed, node, inheritedContext, chunks);
        }
    }

    private String openingContext(ParsedFile parsed, TSNode node) {
        if (node.getNamedChildCount() == 0) {
            return "";
        }
        TSNode first = node.getNamedChild(0);
        return switch (first.getType()) {
            case "start_tag", "script_start_tag", "style_start_tag" ->
                    TreeSitterChunkSupport.source(parsed, node.getStartByte(), first.getEndByte());
            default -> "";
        };
    }

    private boolean isStructuralWrapper(TSNode node) {
        return switch (node.getType()) {
            case "start_tag", "end_tag", "script_start_tag", "script_end_tag",
                    "style_start_tag", "style_end_tag" -> true;
            default -> false;
        };
    }

    private void emitLineChunks(ParsedFile parsed, TSNode node, String context, List<CodeChunk> chunks) {
        String[] lines = TreeSitterChunkSupport.source(parsed, node).split("\\R", -1);
        String prefix = TreeSitterChunkSupport.boundedContext(context);
        StringBuilder current = new StringBuilder(prefix);
        for (String line : lines) {
            if (current.length() + line.length() + 1 > TreeSitterChunkSupport.MAX_CHUNK_CHARACTERS
                    && current.length() > prefix.length()) {
                TreeSitterChunkSupport.addChunk(parsed, node, chunks, current.toString());
                current = new StringBuilder(prefix);
            }
            if (current.length() > prefix.length()) {
                current.append('\n');
            }
            current.append(line);
        }
        TreeSitterChunkSupport.addChunk(parsed, node, chunks, current.toString());
    }
}
