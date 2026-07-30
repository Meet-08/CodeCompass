package com.meet.server.feature.repositoryfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {
}
