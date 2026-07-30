package com.meet.server.feature.codebase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitService {

    public void clone(String url, String branch, String path) {
        try {
            var git = Git.cloneRepository()
                    .setURI(url)
                    .setBranch(branch)
                    .setDirectory(new File(path))
                    .call();
            git.close();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String path) {
        File file = new File(path);
        file.delete();
    }
}
