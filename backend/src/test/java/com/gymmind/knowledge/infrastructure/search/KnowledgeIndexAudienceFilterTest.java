package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIndexAudienceFilterTest {

    @Test
    void inMemoryKeywordIndexFiltersByAudienceRoles() {
        InMemoryKeywordIndex index = new InMemoryKeywordIndex();
        index.upsert(new IndexedChunk("coach", 7L, 1L, null, "coach only", new float[1024], null, 1, null,
                "TENANT", List.of("COACH")));
        index.upsert(new IndexedChunk("member", 7L, 2L, null, "member only", new float[1024], null, 1, null,
                "TENANT", List.of("MEMBER")));

        var memberFilter = new KnowledgeRecallFilter(1L, Set.of(RoleCode.MEMBER), false);
        var hits = index.search("only", 7L, memberFilter);

        assertThat(hits).extracting(IndexedChunk::id).containsExactly("member");
    }

    @Test
    void legacyChunksWithoutAudienceRolesRemainVisible() {
        InMemoryKeywordIndex index = new InMemoryKeywordIndex();
        index.upsert(new IndexedChunk("legacy", 7L, "legacy text", new float[1024]));

        var hits = index.search("legacy", 7L, new KnowledgeRecallFilter(1L, Set.of(RoleCode.MEMBER), false));

        assertThat(hits).extracting(IndexedChunk::id).containsExactly("legacy");
    }
}
