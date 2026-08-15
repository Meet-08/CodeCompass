package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.ChunkType;
import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import org.springframework.stereotype.Component;
import org.treesitter.TSNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class CssExtractor implements ChunkExtractor {

    @Override
    public boolean supports(Language language) {
        return language == Language.CSS;
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
            TreeSitterChunkSupport.addChunk(parsed, node, chunks, complete,
                    ChunkType.CSS_RULE, null, null, null);
            return;
        }

        TSNode block = blockChild(node);
        String context = block == null
                ? TreeSitterChunkSupport.boundedContext(inheritedContext)
                : TreeSitterChunkSupport.boundedContext(inheritedContext
                + TreeSitterChunkSupport.source(parsed, node.getStartByte(), block.getStartByte()));
        int before = chunks.size();
        if (block != null) {
            for (int i = 0; i < block.getNamedChildCount(); i++) {
                emit(parsed, block.getNamedChild(i), context, chunks);
            }
        }
        if (chunks.size() == before) {
            for (int i = 0; i < node.getNamedChildCount(); i++) {
                TSNode child = node.getNamedChild(i);
                if (child != block) {
                    emit(parsed, child, context, chunks);
                }
            }
        }
        if (chunks.size() == before) {
            emitLineChunks(parsed, node, inheritedContext, chunks);
        }
    }

    private TSNode blockChild(TSNode node) {
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            TSNode child = node.getNamedChild(i);
            if (switch (child.getType()) {
                case "block", "declaration_block", "keyframes_block" -> true;
                default -> false;
            }) {
                return child;
            }
        }
        return null;
    }

    private void emitLineChunks(ParsedFile parsed, TSNode node, String context, List<CodeChunk> chunks) {
        String[] lines = TreeSitterChunkSupport.source(parsed, node).split("\\R", -1);
        String prefix = TreeSitterChunkSupport.boundedContext(context);
        StringBuilder current = new StringBuilder(prefix);
        for (String line : lines) {
            if (current.length() + line.length() + 1 > TreeSitterChunkSupport.MAX_CHUNK_CHARACTERS
                    && current.length() > prefix.length()) {
                TreeSitterChunkSupport.addChunk(parsed, node, chunks, current.toString(),
                        ChunkType.CSS_RULE, null, null, null);
                current = new StringBuilder(prefix);
            }
            if (current.length() > prefix.length()) {
                current.append('\n');
            }
            current.append(line);
        }
        TreeSitterChunkSupport.addChunk(parsed, node, chunks, current.toString(),
                ChunkType.CSS_RULE, null, null, null);
    }
}
