package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.iam.domain.model.RoleCode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 受众过滤：索引无 audienceRoles 时视为旧数据放行。 */
public final class KnowledgeAudienceFilters {

    private KnowledgeAudienceFilters() {
    }

    public static List<IndexedChunk> apply(List<IndexedChunk> chunks, KnowledgeRecallFilter filter) {
        if (chunks == null || chunks.isEmpty() || filter == null || filter.skipAudienceFilter()) {
            return chunks == null ? List.of() : chunks;
        }
        return chunks.stream().filter(chunk -> chunk.visibleToAudience(filter.actorRoles())).toList();
    }

    public static List<String> actorRoleNames(Set<RoleCode> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(RoleCode::name).collect(Collectors.toList());
    }
}
