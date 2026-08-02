package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.indexing.parser.ParsedFile;
import com.meet.server.feature.indexing.parser.TreeSitterParser;
import com.meet.server.feature.repositoryfile.RepositoryFile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TreeSitterExtractorTest {

    private final TreeSitterParser parser = new TreeSitterParser();
    private final TreeSitterExtractor extractor = new TreeSitterExtractor();

    @Test
    void ignoresPackageAndImports() {
        var chunks = extract("""
                package com.example;
                import java.util.List;

                public class Demo {
                    public List<String> names() {
                        return List.of("demo");
                    }
                }
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().noneMatch(chunk -> chunk.getContent().contains("import java.util.List")));
        assertTrue(chunks.stream().noneMatch(chunk -> chunk.getContent().contains("package com.example")));
    }

    @Test
    void splitsLargeClassIntoBoundedMemberChunksWithContext() {
        var source = new StringBuilder("public class LargeService {\n");
        for (int i = 0; i < 40; i++) {
            source.append("    public String method").append(i).append("() {\n")
                    .append("        return \"").append("x".repeat(130)).append("\";\n")
                    .append("    }\n");
        }
        source.append("}\n");

        var chunks = extract(source.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= 4_000));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().contains("LargeService")));
        assertEquals(0, chunks.getFirst().getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
    }

    @Test
    void preservesTsxAsCompleteComponentSource() {
        var chunks = extract("Demo.tsx", """
                import { useState } from "react";

                type Props = { title: string };

                export function Demo({ title }: Props) {
                    const [open, setOpen] = useState(false);
                    return (
                        <section className=\"panel\">
                            <h1>{title}</h1>
                            <button onClick={() => setOpen(!open)}>{open ? \"Close\" : \"Open\"}</button>
                        </section>
                    );
                }
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("<section className=\"panel\">")
                && chunk.getContent().contains("<h1>{title}</h1>")
                && chunk.getContent().contains("</section>")));
        assertTrue(chunks.stream().noneMatch(chunk -> chunk.getContent().equals("Demo")
                || chunk.getContent().equals("()")
                || chunk.getContent().equals("className")));
    }

    @Test
    void preservesJsxAsCompleteComponentSource() {
        var chunks = extract("Demo.jsx", """
                export default function Demo() {
                    return (
                        <main>
                            <Header />
                            <p>Hello {name}</p>
                        </main>
                    );
                }
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getContent().contains("<Header />")
                && chunk.getContent().contains("<p>Hello {name}</p>")
                && chunk.getContent().contains("</main>")));
    }

    private java.util.List<com.meet.server.feature.codechunk.CodeChunk> extract(String content) {
        return extract("Demo.java", content);
    }

    private java.util.List<com.meet.server.feature.codechunk.CodeChunk> extract(String path, String content) {
        var codebase = Codebase.builder().id(UUID.randomUUID()).name("test").build();
        var file = RepositoryFile.builder()
                .id(UUID.randomUUID())
                .codebase(codebase)
                .path(path)
                .language(path.substring(path.lastIndexOf('.') + 1))
                .build();
        ParsedFile parsed = parser.parse(file, content);
        return extractor.extract(parsed);
    }
}
