package com.meet.server.feature.repositoryfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {

    long countByCodebaseId(UUID codebaseId);

    @Modifying
    @Transactional
    @Query("delete from RepositoryFile f where f.codebase.id = :codebaseId")
    int deleteByCodebaseId(@Param("codebaseId") UUID codebaseId);
}
