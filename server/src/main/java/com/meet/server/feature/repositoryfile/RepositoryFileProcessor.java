package com.meet.server.feature.repositoryfile;

import com.meet.server.feature.codebase.Codebase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class RepositoryFileProcessor {

    private static final Logger log = LogManager.getLogger(RepositoryFileProcessor.class);

    public int process(Codebase codebase, Path repositoryPath, List<RepositoryFileDescriptor> files) {
        files.forEach(file -> log.info("Codebase {} file: {} ({} bytes)", codebase.getId(), file.path(), file.size()));
        return files.size();
    }
}
