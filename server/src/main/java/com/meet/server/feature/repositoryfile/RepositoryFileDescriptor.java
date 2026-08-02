package com.meet.server.feature.repositoryfile;

public record RepositoryFileDescriptor(
        String path,
        String language,
        long size,
        String checksum
) {
}
