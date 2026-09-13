package com.gymmind.ai.domain; import java.util.List; public interface RerankGateway { List<RankedChunk> rerank(String query,List<ContextSegment> candidates); }
