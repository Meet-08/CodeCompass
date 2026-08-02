package com.meet.server.feature.codebase;

import com.meet.server.common.exception.CodebaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodebaseStatusService {

    private final CodebaseRepository codebaseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(UUID codebaseId, CodebaseStatus status) {
        var codebase = codebaseRepository.findById(codebaseId)
                .orElseThrow(() -> new CodebaseException(
                        "CODEBASE_NOT_FOUND",
                        "Codebase not found",
                        HttpStatus.NOT_FOUND));

        codebase.setStatus(status);
        if (status == CodebaseStatus.INDEXED) {
            codebase.setIndexedAt(Instant.now());
        }
        codebaseRepository.save(codebase);
    }
}
