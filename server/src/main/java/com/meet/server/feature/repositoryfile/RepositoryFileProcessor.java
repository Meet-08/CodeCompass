package com.meet.server.feature.repositoryfile;

import com.meet.server.feature.codebase.Codebase;
import com.meet.server.feature.embedding.EmbeddingService;
import com.meet.server.feature.indexing.extractor.ChunkExtractor;
import com.meet.server.feature.indexing.language.Language;
import com.meet.server.feature.indexing.parser.ParsedFile;
import com.meet.server.feature.indexing.parser.Parser;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryFileProcessor {

    private static final Logger log = LogManager.getLogger(RepositoryFileProcessor.class);

    private final RepositoryFileRepository repositoryFileRepository;
    private final EmbeddingService embeddingService;
    private final List<Parser> parsers;
    private final List<ChunkExtractor> extractors;

    public int process(Codebase codebase, Path repositoryPath, List<RepositoryFileDescriptor> files,
                       String commitSha) {
        log.debug("Processing {} files for codebase {}", files.size(), codebase.getId());
        files.forEach(file -> processFile(codebase, repositoryPath, file, commitSha));
        log.debug("Processed {} files for codebase {}", files.size(), codebase.getId());
        return files.size();
    }

    private void processFile(Codebase codebase, Path repositoryPath, RepositoryFileDescriptor descriptor,
                             String commitSha) {
        if (isImageOrVideo(descriptor.path())) {
            return;
        }

        var repositoryFile = repositoryFileRepository.save(RepositoryFile.builder()
                .codebase(codebase)
                .path(descriptor.path())
                .language(descriptor.language())
                .checksum(descriptor.checksum())
                .size(descriptor.size())
                .build());
        try {
            var content = Files.readString(repositoryPath.resolve(descriptor.path()));
            var language = Language.from(descriptor.language());
            Parser parser = parsers.stream()
                    .filter(candidate -> candidate.supports(language))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No parser configured for " + language));
            ParsedFile parsed = parser.parse(repositoryFile, content);
            var extractor = extractors.stream()
                    .filter(candidate -> candidate.supports(parsed.language()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No extractor configured for " + parsed.language()));
            var chunks = extractor.extract(parsed);
            chunks.forEach(chunk -> chunk.setCommitSha(commitSha));
            embeddingService.embedChunks(chunks);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read repository file: " + descriptor.path(), exception);
        }
    }

    private boolean isImageOrVideo(String path) {
        var lowerPath = path.toLowerCase(java.util.Locale.ROOT);
        return java.util.Set.of(
                ".apng", ".avif", ".bmp", ".gif", ".heic", ".jpeg", ".jpg", ".png", ".svg", ".webp",
                ".avi", ".m4v", ".mkv", ".mov", ".mp4", ".mpeg", ".mpg", ".webm"
        ).stream().anyMatch(lowerPath::endsWith);
    }
}
