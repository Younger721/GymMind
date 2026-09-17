package com.gymmind.ai.harness.plugins;

import com.gymmind.ai.application.AiMemoryService;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 用户 AI 记忆画像插件 */
@Component
public class AiMemoryHarnessPlugin implements AiHarnessPlugin {

    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "ai_memory",
            "AI 记忆",
            "注入用户长期记忆与偏好",
            "MEMORY",
            "ai:memory",
            true,
            false);

    private final AiMemoryService memory;

    public AiMemoryHarnessPlugin(AiMemoryService memory) {
        this.memory = memory;
    }

    @Override
    public AiHarnessPluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
        var profile = memory.context(actor);
        if (profile == null || profile.isEmpty()) {
            return;
        }
        String text = profile.stream()
                .map(e -> e.key() + "=" + e.value())
                .reduce((a, b) -> a + "；" + b)
                .orElse("");
        context.add(new ContextSegment("memory:profile", "user_profile", text));
    }
}
