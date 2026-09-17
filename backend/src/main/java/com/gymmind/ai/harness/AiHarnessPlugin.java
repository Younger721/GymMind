package com.gymmind.ai.harness;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;

/** 可插拔 AI 能力：向 Harness 会话贡献上下文或副作用 */
public interface AiHarnessPlugin {

    AiHarnessPluginDescriptor descriptor();

    /** 是否对当前 Actor 可用（租户/权限） */
    default boolean supports(CurrentActor actor) {
        AiHarnessPluginDescriptor d = descriptor();
        if (d.requiresTenant() && actor.tenantId() == null) {
            return false;
        }
        if (d.permission() == null || d.permission().isBlank()) {
            return true;
        }
        return actor.hasPermission(d.permission());
    }

    /** 为本次对话贡献 RAG/工具上下文 */
    void contribute(CurrentActor actor, String input, List<ContextSegment> context);
}
