package com.gymmind.ai.harness;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.ai.chat.application.AiRoleProfileResolver;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiHarnessPluginTest {

    @Test
    void tenantPluginSupportsPlatformActorOnlyWithSelectedTenant() {
        AiHarnessPlugin plugin = new AiHarnessPlugin() {
            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return new AiHarnessPluginDescriptor("tenant-tool", "Tenant tool", "", "test",
                        "tool:read", true, false);
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {}
        };
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        TenantContextHolder.clear();
        assertThat(plugin.supports(platform)).isFalse();
        TenantContextHolder.set(11L);
        try {
            assertThat(plugin.supports(platform)).isTrue();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void knowledgeSearchSupportsPlatformActorWithoutTenantContext() {
        KnowledgeSearchService search = (actor, query) -> List.of();
        AiHarnessPlugin plugin = new com.gymmind.ai.harness.plugins.KnowledgeSearchHarnessPlugin(search);
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        TenantContextHolder.clear();

        assertThat(plugin.supports(platform)).isTrue();
    }

    @Test
    void platformRoleProfileKeepsKnowledgeSearchEnabled() {
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        assertThat(new AiRoleProfileResolver().resolve(platform).enabledPlugins())
                .contains("knowledge_search", "platform_ai_admin_metrics");
    }

    @Test
    void platformRoleProfileStaysOnPlatformToolsWhenTenantSelected() {
        CurrentActor platform = new CurrentActor(9L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of("analytics:read"), 0L, "platform");
        TenantContextHolder.set(11L);
        try {
            var profile = new AiRoleProfileResolver().resolve(platform);
            assertThat(profile.enabledPlugins()).contains("knowledge_search", "platform_ai_admin_metrics");
            assertThat(profile.enabledPlugins()).doesNotContain("gym_operations_metrics", "member_churn_risk_list");
            assertThat(profile.quickPrompts()).contains("当前门店本月 AI 配额和用量是多少？");
            assertThat(profile.quickPrompts()).doesNotContain("上个月来了多少会员？");
        } finally {
            TenantContextHolder.clear();
        }
    }
}
