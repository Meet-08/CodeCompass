package com.meet.server.feature.indexing.extractor;

import com.meet.server.feature.codechunk.CodeChunk;
import com.meet.server.feature.indexing.language.Language;

import java.util.List;

public interface ChunkExtractor {

    boolean supports(Language language);

    List<CodeChunk> extract(ParsedFile parsed);

}
