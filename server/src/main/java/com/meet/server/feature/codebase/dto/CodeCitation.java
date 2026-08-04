package com.meet.server.feature.codebase.dto;

import java.util.UUID;

public record CodeCitation(UUID chunkId, String path, Integer startLine, Integer endLine, String language, double distance) {}
