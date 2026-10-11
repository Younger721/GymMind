package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;

import java.util.Set;

/** 召回时的租户隔离与受众过滤上下文。 */
public record KnowledgeRecallFilter(Long userId, Set<RoleCode> actorRoles, boolean platformAdmin) {

    public static KnowledgeRecallFilter of(CurrentActor actor) {
        if (actor == null) {
            return new KnowledgeRecallFilter(null, Set.of(), false);
        }
        return new KnowledgeRecallFilter(actor.userId(), actor.roles(), actor.isPlatformAdmin());
    }

    public boolean skipAudienceFilter() {
        return platformAdmin;
    }
}
