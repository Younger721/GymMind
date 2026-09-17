package com.gymmind.ai.harness.application;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.application.AiUsageCommand;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.DefaultAiUsageService;
import com.gymmind.ai.application.SourceCitation;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.PromptInjectionGuard;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.ai.harness.AiHarnessPluginRegistry;
import com.gymmind.ai.harness.HarnessPluginList;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.PlatformHarnessConfig;
import com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultAiHarnessService implements AiHarnessService {

    private final AiHarnessPluginRegistry registry;
    private final TenantHarnessConfigRepository tenantConfigs;
    private final PlatformHarnessConfigRepository platformConfigs;
    private final AiHarnessProperties properties;
    private final ChatModelGateway model;
    private final DefaultAiUsageService usage;
    private final TenantQuotaService quotas;
    private final PromptInjectionGuard guard = new PromptInjectionGuard();

    public DefaultAiHarnessService(
            AiHarnessPluginRegistry registry,
            TenantHarnessConfigRepository tenantConfigs,
            PlatformHarnessConfigRepository platformConfigs,
            AiHarnessProperties properties,
            ChatModelGateway model,
            DefaultAiUsageService usage,
            TenantQuotaService quotas) {
        this.registry = registry;
        this.tenantConfigs = tenantConfigs;
        this.platformConfigs = platformConfigs;
        this.properties = properties;
        this.model = model;
        this.usage = usage;
        this.quotas = quotas;
    }

    @Override
    @Transactional(readOnly = true)
    public HarnessConfigView getConfig(CurrentActor actor) {
        if (actor.isPlatformAdmin()) {
            return buildPlatformView(actor);
        }
        requireTenantRead(actor);
        TenantHarnessConfig config = requireTenantConfig(actor.tenantId());
        return buildTenantView(actor, config);
    }

    @Override
    @Transactional
    public HarnessConfigView updateConfig(CurrentActor actor, UpdateHarnessConfigCommand command) {
        requireTenant(actor, "tenant:settings:write");
        validateCommand(command);
        TenantHarnessConfig config = requireTenantConfig(actor.tenantId());
        List<String> enabled = sanitizePlugins(actor, command.enabledPlugins());
        config.update(HarnessPluginList.join(enabled), normalizePrompt(command.systemPrompt()));
        tenantConfigs.save(config);
        return buildTenantView(actor, config);
    }

    @Override
    @Transactional(readOnly = true)
    public HarnessConfigView getPlatformConfig(CurrentActor actor) {
        return buildPlatformView(requirePlatform(actor, "platform:tenant:read"));
    }

    @Override
    @Transactional
    public HarnessConfigView updatePlatformConfig(CurrentActor actor, UpdateHarnessConfigCommand command) {
        requirePlatform(actor, "platform:tenant:write");
        validateCommand(command);
        PlatformHarnessConfig config = requirePlatformConfig();
        List<String> enabled = sanitizePlugins(actor, command.enabledPlugins());
        config.update(HarnessPluginList.join(enabled), normalizePrompt(command.systemPrompt()));
        platformConfigs.save(config);
        return buildPlatformView(actor);
    }

    @Override
    @Transactional(readOnly = true)
    public AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins) {
        if (sessionId == null || sessionId.isBlank() || question == null || question.isBlank()
                || guard.isBlocked(question)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!canChat(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (actor.tenantId() != null && quotas != null) {
            quotas.ensureAiChatAllowed(actor.tenantId());
        }

        List<String> enabled = resolveEnabledPlugins(actor, sessionPlugins);
        if (enabled.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        String systemPrompt = resolveSystemPrompt(actor);
        List<ContextSegment> context = new ArrayList<>();
        for (String pluginId : enabled) {
            if ("basic_chat".equals(pluginId)) {
                continue;
            }
            registry.find(pluginId).ifPresent(plugin -> {
                try {
                    plugin.contribute(actor, question, context);
                } catch (RuntimeException ignored) {
                    // 单个插件失败不阻断 Harness
                }
            });
        }

        long started = System.currentTimeMillis();
        if (context.isEmpty() && !enabled.contains("basic_chat")) {
            recordUsage(actor, started, "NO_CONTEXT");
            return new AiChatView("没有找到足够可靠的知识依据，暂不提供确定性建议。", List.of(), List.of());
        }

        String prompt = "【系统提示】\n" + systemPrompt + "\n【用户问题】\n" + question;
        String answer = model.complete(prompt, context);
        if (answer == null || answer.isBlank()) {
            answer = "模型暂时不可用，请稍后重试。";
        }
        recordUsage(actor, started, "OK");

        var sources = context.stream().map(ContextSegment::id).filter(Objects::nonNull).distinct().toList();
        var citations = context.stream()
                .filter(item -> item.id() != null)
                .map(item -> new SourceCitation(item.id(), item.documentId(), item.text()))
                .distinct()
                .toList();
        return new AiChatView(answer, sources, citations);
    }

    private HarnessConfigView buildTenantView(CurrentActor actor, TenantHarnessConfig config) {
        return new HarnessConfigView(
                HarnessPluginList.parse(config.getEnabledPlugins()),
                registry.listFor(actor),
                config.getSystemPrompt(),
                false);
    }

    private HarnessConfigView buildPlatformView(CurrentActor actor) {
        PlatformHarnessConfig config = requirePlatformConfig();
        return new HarnessConfigView(
                HarnessPluginList.parse(config.getEnabledPlugins()),
                registry.listFor(actor),
                config.getSystemPrompt(),
                true);
    }

    private TenantHarnessConfig requireTenantConfig(Long tenantId) {
        return tenantConfigs.findByTenantId(tenantId).orElseGet(() -> tenantConfigs.save(
                TenantHarnessConfig.defaults(
                        tenantId,
                        HarnessPluginList.join(properties.getTenantDefaultPlugins()),
                        properties.getDefaultSystemPrompt())));
    }

    private PlatformHarnessConfig requirePlatformConfig() {
        return platformConfigs.findSingleton().orElseGet(() -> platformConfigs.save(
                new PlatformHarnessConfig(
                        HarnessPluginList.join(properties.getPlatformDefaultPlugins()),
                        properties.getDefaultSystemPrompt())));
    }

    private List<String> resolveEnabledPlugins(CurrentActor actor, List<String> sessionPlugins) {
        List<String> configured = actor.isPlatformAdmin()
                ? HarnessPluginList.parse(requirePlatformConfig().getEnabledPlugins())
                : HarnessPluginList.parse(requireTenantConfig(actor.tenantId()).getEnabledPlugins());
        List<String> allowed = registry.listFor(actor).stream().map(AiHarnessPluginDescriptor::id).toList();
        List<String> base = configured;
        if (sessionPlugins != null && !sessionPlugins.isEmpty()) {
            base = HarnessPluginList.intersect(sessionPlugins, configured);
        }
        return HarnessPluginList.intersect(base, allowed);
    }

    private String resolveSystemPrompt(CurrentActor actor) {
        if (actor.isPlatformAdmin()) {
            return requirePlatformConfig().getSystemPrompt();
        }
        return requireTenantConfig(actor.tenantId()).getSystemPrompt();
    }

    private List<String> sanitizePlugins(CurrentActor actor, List<String> requested) {
        List<String> allowed = registry.listFor(actor).stream().map(AiHarnessPluginDescriptor::id).toList();
        List<String> picked = requested == null || requested.isEmpty()
                ? List.of("basic_chat")
                : requested;
        List<String> result = HarnessPluginList.intersect(picked, allowed);
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return result;
    }

    private static boolean canChat(CurrentActor actor) {
        if (actor == null) {
            return false;
        }
        if (actor.isPlatformAdmin()) {
            return true;
        }
        return actor.tenantId() != null
                && (actor.hasPermission("ai:chat") || actor.hasPermission("agent:chat"));
    }

    private static void validateCommand(UpdateHarnessConfigCommand command) {
        if (command == null || command.systemPrompt() == null || command.systemPrompt().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static String normalizePrompt(String prompt) {
        return prompt.trim();
    }

    private static void requireTenant(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireTenantRead(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (actor.hasPermission("tenant:settings:read")
                || actor.hasPermission("ai:chat")
                || actor.hasPermission("agent:chat")) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private static CurrentActor requirePlatform(CurrentActor actor, String permission) {
        if (actor == null || !actor.isPlatformAdmin() || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return actor;
    }

    private void recordUsage(CurrentActor actor, long started, String status) {
        if (usage == null || actor.tenantId() == null) {
            return;
        }
        try {
            usage.record(actor, new AiUsageCommand(
                    "harness",
                    0,
                    0,
                    Math.max(0, System.currentTimeMillis() - started),
                    "harness",
                    status,
                    null));
        } catch (RuntimeException ignored) {
            // 用量记录失败不影响对话
        }
    }
}
