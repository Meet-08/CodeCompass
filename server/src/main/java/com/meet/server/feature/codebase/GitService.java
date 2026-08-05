package com.meet.server.feature.codebase;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.repositoryfile.RepositoryFileDescriptor;
import com.meet.server.feature.indexing.language.Language;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class GitService {

    private static final int CLONE_TIMEOUT_SECONDS = 60;
    private static final int CLEANUP_ATTEMPTS = 5;
    private static final long CLEANUP_RETRY_DELAY_MILLIS = 250L;

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".github", ".gitlab", ".circleci", ".idea", ".vscode",
            "node_modules", "bower_components", "vendor", "target", "build", "out",
            "dist", "coverage", ".gradle", ".next", ".nuxt", ".angular", "__pycache__",
            ".pytest_cache", ".mypy_cache", ".tox", ".venv", "venv", "env", "bin", "obj"
    );

    private static final Set<String> IGNORED_FILE_NAMES = Set.of(
            ".gitignore", ".gitattributes", ".gitmodules", ".dockerignore",
            ".editorconfig", ".env", ".env.local", ".env.development", ".env.production",
            "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "bun.lockb",
            "composer.lock", "gemfile.lock", "poetry.lock", "pipfile.lock", "cargo.lock",
            "gradle.lockfile", "go.sum"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            "7z", "avi", "bmp", "class", "dll", "dmg", "exe", "flac", "gif", "ico",
            "jar", "jpeg", "jpg", "m4v", "mkv", "mov", "mp3", "mp4", "mpeg", "mpg", "png", "webm", "svg",
            "tar", "ttf", "wav", "webp", "woff", "woff2", "zip", "map", "min.js", "min.css"
    );

    public void cloneRepository(String url, String branch, Path path) {
        try (var git = Git.cloneRepository()
                    .setURI(url)
                    .setBranch(branch == null || branch.isBlank() ? "main" : branch)
                    .setDirectory(path.toFile())
                    .setTimeout(CLONE_TIMEOUT_SECONDS)
                    .call()) {
            // The cloned repository is closed when this operation completes.
        } catch (GitAPIException e) {
            throw new CodebaseException(
                    "CODEBASE_CLONE_FAILED",
                    "Unable to clone repository",
                    HttpStatus.BAD_GATEWAY,
                    e);
        }
    }

    public List<RepositoryFileDescriptor> listFiles(Path repositoryPath) {
        var ignoreNodes = loadIgnoreNodes(repositoryPath);

        try (var paths = Files.walk(repositoryPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> shouldIndex(repositoryPath, path, ignoreNodes))
                    .map(path -> descriptor(repositoryPath, path))
                    .toList();
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_FILE_LIST_FAILED",
                    "Unable to list repository files",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    public String currentCommitSha(Path repositoryPath) {
        try (var git = Git.open(repositoryPath.toFile())) {
            ObjectId head = git.getRepository().resolve("HEAD");
            if (head == null) {
                throw new IOException("Repository HEAD is not available");
            }
            return head.name();
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_COMMIT_SHA_FAILED",
                    "Unable to resolve repository commit",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private Map<Path, IgnoreNode> loadIgnoreNodes(Path repositoryPath) {
        try (var paths = Files.walk(repositoryPath)) {
            return paths.filter(Files::isDirectory)
                    .map(Path::normalize)
                    .filter(path -> Files.isRegularFile(path.resolve(".gitignore")))
                    .collect(LinkedHashMap::new, (nodes, path) -> nodes.put(path, loadIgnoreNode(path)), Map::putAll);
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_IGNORE_FILE_FAILED",
                    "Unable to read repository ignore rules",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private IgnoreNode loadIgnoreNode(Path directory) {
        var ignoreNode = new IgnoreNode();
        try (InputStream input = Files.newInputStream(directory.resolve(".gitignore"))) {
            ignoreNode.parse(input);
            return ignoreNode;
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_IGNORE_FILE_FAILED",
                    "Unable to read repository ignore rules",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private boolean shouldIndex(Path repositoryPath, Path file, Map<Path, IgnoreNode> ignoreNodes) {
        var relativePath = repositoryPath.relativize(file).toString().replace('\\', '/');
        var pathParts = relativePath.split("/");
        var fileName = pathParts[pathParts.length - 1].toLowerCase(Locale.ROOT);

        for (int index = 0; index < pathParts.length - 1; index++) {
            if (IGNORED_DIRECTORIES.contains(pathParts[index].toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        if (IGNORED_FILE_NAMES.contains(fileName) || isIgnoredExtension(fileName)) {
            return false;
        }

        for (Path directory = repositoryPath; directory != null && directory.startsWith(repositoryPath);
             directory = directory.equals(repositoryPath) ? null : directory.getParent()) {
            var ignoreNode = ignoreNodes.get(directory.normalize());
            if (ignoreNode != null) {
                var ignoredPath = directory.relativize(file).toString().replace('\\', '/');
                var pathPartsFromDirectory = ignoredPath.split("/");
                StringBuilder path = new StringBuilder();
                for (int index = 0; index < pathPartsFromDirectory.length - 1; index++) {
                    if (path.length() > 0) {
                        path.append('/');
                    }
                    path.append(pathPartsFromDirectory[index]);
                    if (Boolean.TRUE.equals(ignoreNode.checkIgnored(path.toString(), true))) {
                        return false;
                    }
                }
                if (Boolean.TRUE.equals(ignoreNode.checkIgnored(ignoredPath, false))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isIgnoredExtension(String fileName) {
        var dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }

        var extension = fileName.substring(dot + 1);
        return IGNORED_EXTENSIONS.contains(extension)
                || IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    public void deleteRepository(Path repositoryPath) {
        if (repositoryPath == null || !Files.exists(repositoryPath)) {
            return;
        }

        try (var paths = Files.walk(repositoryPath)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_CLEANUP_FAILED",
                    "Unable to delete cloned repository",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private RepositoryFileDescriptor descriptor(Path repositoryPath, Path file) {
        try {
            var relativePath = repositoryPath.relativize(file).toString().replace('\\', '/');
            return new RepositoryFileDescriptor(
                    relativePath,
                    Language.extensionOf(relativePath),
                    Files.size(file),
                    sha256(file));
        } catch (IOException e) {
            throw new CodebaseException(
                    "CODEBASE_FILE_INSPECTION_FAILED",
                    "Unable to inspect repository file",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private String sha256(Path file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                var buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CodebaseException(
                    "CODEBASE_CHECKSUM_FAILED",
                    "Unable to checksum repository file",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private void deletePath(Path path) {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= CLEANUP_ATTEMPTS; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException exception) {
                lastFailure = exception;
                if (attempt == CLEANUP_ATTEMPTS) {
                    break;
                }
                try {
                    Thread.sleep(CLEANUP_RETRY_DELAY_MILLIS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    lastFailure = new IOException("Repository cleanup interrupted", interruptedException);
                    break;
                }
            }
        }

        throw new CodebaseException(
                "CODEBASE_CLEANUP_FAILED",
                "Unable to delete cloned repository path",
                HttpStatus.INTERNAL_SERVER_ERROR,
                lastFailure);
    }
}
