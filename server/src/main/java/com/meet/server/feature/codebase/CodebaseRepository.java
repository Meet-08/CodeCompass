package com.meet.server.feature.codebase;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Repository
public interface CodebaseRepository extends JpaRepository<Codebase, UUID> {

    long countByUserId(UUID userId);

    @Query("""
            select new com.meet.server.feature.codebase.dto.CodebaseResponse(
                c.id, c.name, c.cloneUrl, c.branch, c.status, c.lastCommitSha,
                c.indexedAt, c.createdAt, c.updatedAt, count(f.id))
            from Codebase c left join RepositoryFile f on f.codebase.id = c.id
            where c.user.id = :userId
            group by c.id, c.name, c.cloneUrl, c.branch, c.status, c.lastCommitSha,
                     c.indexedAt, c.createdAt, c.updatedAt
            order by c.createdAt desc
            """)
    List<com.meet.server.feature.codebase.dto.CodebaseResponse> findResponsesByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("update Codebase c set c.lastCommitSha = :lastCommitSha where c.id = :codebaseId")
    int updateLastCommitSha(@Param("codebaseId") UUID codebaseId,
                            @Param("lastCommitSha") String lastCommitSha);
}
