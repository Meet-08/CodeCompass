package com.meet.server.feature.codebase;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface CodebaseRepository extends JpaRepository<Codebase, UUID> {

    @Modifying
    @Transactional
    @Query("update Codebase c set c.lastCommitSha = :lastCommitSha where c.id = :codebaseId")
    int updateLastCommitSha(@Param("codebaseId") UUID codebaseId,
                            @Param("lastCommitSha") String lastCommitSha);
}
