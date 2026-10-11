package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentVisibility;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 索引层受众与可见性字段编码。 */
public final class KnowledgeIndexAudience {

    private KnowledgeIndexAudience() {
    }

    public static String visibility(DocumentVisibility visibility) {
        return visibility == null ? DocumentVisibility.TENANT.name() : visibility.name();
    }

    public static List<String> roleNames(Set<RoleCode> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(RoleCode::name).sorted().toList();
    }
}
