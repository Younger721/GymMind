package com.gymmind.ai.harness;

import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 插件注册表：Spring 自动收集所有 {@link AiHarnessPlugin} 实现 */
@Component
public class AiHarnessPluginRegistry {

    private final Map<String, AiHarnessPlugin> plugins = new LinkedHashMap<>();

    public AiHarnessPluginRegistry(List<AiHarnessPlugin> discovered) {
        for (AiHarnessPlugin plugin : discovered) {
            plugins.put(plugin.descriptor().id(), plugin);
        }
    }

    public Collection<AiHarnessPlugin> all() {
        return plugins.values();
    }

    public Optional<AiHarnessPlugin> find(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    public List<AiHarnessPluginDescriptor> listFor(CurrentActor actor) {
        return plugins.values().stream()
                .filter(p -> p.supports(actor))
                .map(AiHarnessPlugin::descriptor)
                .toList();
    }
}
