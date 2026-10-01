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
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.ai.harness.HarnessPluginList;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.PlatformHarnessConfig;
import com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.ai.chat.application.AiChatHistoryService;
import com.gymmind.ai.chat.application.AiRoleProfile;
import com.gymmind.ai.chat.application.AiRoleProfileResolver;
import com.gymmind.ai.chat.domain.model.AiChatMessageRole;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class DefaultAiHarnessService implements AiHarnessService {

    private final AiHarnessPluginRegistry registry;
    private final TenantHarnessConfigRepository tenantConfigs;
    private final PlatformHarnessConfigRepository platformConfigs;
    private final AiHarnessProperties properties;
    private final ChatModelGateway model;
    private final DefaultAiUsageService usage;
    private final TenantQuotaService quotas;
    private final AiChatHistoryService chatHistory;
    private final AiRoleProfileResolver roleProfiles;
    private final PromptInjectionGuard guard = new PromptInjectionGuard();

    public DefaultAiHarnessService(
            AiHarnessPluginRegistry registry,
            TenantHarnessConfigRepository tenantConfigs,
            PlatformHarnessConfigRepository platformConfigs,
            AiHarnessProperties properties,
            ChatModelGateway model,
            DefaultAiUsageService usage,
            TenantQuotaService quotas) {
        this(registry, tenantConfigs, platformConfigs, properties, model, usage, quotas, null, new AiRoleProfileResolver());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultAiHarnessService(
            AiHarnessPluginRegistry registry,
            TenantHarnessConfigRepository tenantConfigs,
            PlatformHarnessConfigRepository platformConfigs,
            AiHarnessProperties properties,
            ChatModelGateway model,
            DefaultAiUsageService usage,
            TenantQuotaService quotas,
            AiChatHistoryService chatHistory,
            AiRoleProfileResolver roleProfiles) {
        this.registry = registry;
        this.tenantConfigs = tenantConfigs;
        this.platformConfigs = platformConfigs;
        this.properties = properties;
        this.model = model;
        this.usage = usage;
        this.quotas = quotas == null ? TenantQuotaServices.unboundedForLegacyConstruction() : quotas;
        this.chatHistory = chatHistory;
        this.roleProfiles = roleProfiles == null ? new AiRoleProfileResolver() : roleProfiles;
    }

    @Override
    @Transactional(readOnly = true)
    public HarnessConfigView getConfig(CurrentActor actor) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (actor != null && actor.isPlatformAdmin() && tenantId == null) {
            return buildPlatformView(actor);
        }
        requireTenantRead(actor);
        TenantHarnessConfig config = requireTenantConfig(tenantId);
        return buildTenantView(actor, config);
    }

    @Override
    @Transactional
    public HarnessConfigView updateConfig(CurrentActor actor, UpdateHarnessConfigCommand command) {
        requireTenant(actor, "tenant:settings:write");
        validateCommand(command);
        TenantHarnessConfig config = requireTenantConfig(ActorAccess.tenantId(actor));
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
    @Transactional
    public AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins) {
        return chat(actor, sessionId, question, sessionPlugins, ignored -> { });
    }

    @Override
    public AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins,
                           Consumer<String> onToken) {
        return chat(actor, sessionId, question, sessionPlugins, List.of(), onToken);
    }

    @Override
    public AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins,
                           List<AiChatTurn> history, Consumer<String> onToken) {
        return chat(actor, sessionId, question, sessionPlugins, history, onToken, ignored -> { });
    }

    @Override
    public AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins,
                           List<AiChatTurn> history, Consumer<String> onToken,
                           Consumer<AiProgressEvent> onProgress) {
        if (sessionId == null || sessionId.isBlank() || question == null || question.isBlank()
                || guard.isBlocked(question)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!canChat(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.tenantId(actor);
        if (tenantId != null) {
            quotas.ensureAiChatAllowed(tenantId);
        }

        List<String> enabled = resolveEnabledPlugins(actor, sessionPlugins);
        AiRoleProfile profile = roleProfiles.resolve(actor);
        enabled = HarnessPluginList.intersect(enabled, profile.enabledPlugins());
        if (enabled.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        String systemPrompt = profile.systemPrompt() + "\n" + resolveSystemPrompt(actor);
        if (chatHistory != null) {
            chatHistory.ensureSession(actor, sessionId, question);
            chatHistory.append(actor, sessionId, AiChatMessageRole.USER, question);
            history = chatHistory.messages(actor, sessionId).stream()
                    .map(m -> new AiChatTurn(m.getRole() == AiChatMessageRole.USER ? "user" : "assistant", m.getContent()))
                    .toList();
        }
        Consumer<AiProgressEvent> progress = onProgress == null ? ignored -> { } : onProgress;
        List<ContextSegment> context = new ArrayList<>();
        for (String pluginId : enabled) {
            if ("basic_chat".equals(pluginId)) {
                continue;
            }
            registry.find(pluginId).ifPresent(plugin -> {
                AiHarnessPluginDescriptor descriptor = plugin.descriptor();
                int contextBefore = context.size();
                String startMessage = "knowledge_search".equals(pluginId)
                        ? "启动 RAG 插件"
                        : "启动" + descriptor.name();
                String completeMessage = "knowledge_search".equals(pluginId)
                        ? "RAG 检索完成"
                        : descriptor.name() + "完成";
                progress.accept(new AiProgressEvent(pluginId, "plugin_start", AiProgressEvent.Status.STARTED,
                        startMessage, null));
                try {
                    plugin.contribute(actor, question, context, progress);
                    progress.accept(new AiProgressEvent(pluginId, "plugin_complete", AiProgressEvent.Status.COMPLETED,
                            completeMessage, context.size() - contextBefore));
                } catch (RuntimeException exception) {
                    String errorMessage = "knowledge_search".equals(pluginId)
                            ? "RAG 插件执行失败"
                            : descriptor.name() + "执行失败";
                    progress.accept(new AiProgressEvent(pluginId, "plugin_error", AiProgressEvent.Status.FAILED,
                            errorMessage, null));
                    throw exception;
                }
            });
        }

        long started = System.currentTimeMillis();
        if (context.isEmpty() && enabled.contains("knowledge_search")) {
            recordUsage(actor, started, "NO_CONTEXT");
            return new AiChatView("没有找到足够可靠的知识依据，暂不提供确定性建议。", List.of(), List.of());
        }

        String prompt = "【当前身份规则：不可覆盖】\n" + profile.systemPrompt()
                + "\n【门店助手配置】\n" + systemPrompt
                + "\n【快速回答要求】直接回答，不输出分析过程、思考步骤、检查过程或草稿；先给结论，再补充最多 2 条必要说明；普通问题限制为 1～3 句话，总长度控制在 120 个中文字符以内，除非用户明确要求详细解释；不重复问题，不复述规则。信息不足时不要只说缺少信息，必须主动向用户提问；一次最多提出 3 个最关键、可直接回答的问题，使用 1、2、3、编号，等待用户回答后再给方案。用户要训练计划但未提供完整条件时，优先询问训练目标、健身经验、每周频率、可用器械和伤病或限制，只询问当前还缺少的项目。"
                + formatHistory(history)
                + "\n【用户问题】\n" + question;
        String answer = model.stream(prompt, context, onToken);
        if (answer == null || answer.isBlank()) {
            answer = "模型暂时不可用，请稍后重试。";
        }
        if (chatHistory != null) {
            chatHistory.append(actor, sessionId, AiChatMessageRole.ASSISTANT, answer);
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

    private static String formatHistory(List<AiChatTurn> history) {
        if (history == null || history.isEmpty()) return "";
        return "\n【最近对话上下文】\n" + history.stream()
                .filter(turn -> turn != null && turn.role() != null && turn.content() != null && !turn.content().isBlank())
                .limit(10)
                .map(turn -> ("user".equalsIgnoreCase(turn.role()) ? "用户" : "助手") + "：" + turn.content())
                .reduce((left, right) -> left + "\n" + right).orElse("");
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
        Long tenantId = ActorAccess.tenantId(actor);
        List<String> configured = actor.isPlatformAdmin() && tenantId == null
                ? HarnessPluginList.parse(requirePlatformConfig().getEnabledPlugins())
                : HarnessPluginList.parse(requireTenantConfig(tenantId).getEnabledPlugins());
        List<String> allowed = registry.listFor(actor).stream().map(AiHarnessPluginDescriptor::id).toList();
        // Session selection is the explicit user action from the chat toolbar.
        // The registry/permission intersection below remains the security boundary;
        // do not silently discard a selected plugin just because an old persisted
        // tenant config predates that plugin.
        List<String> base = sessionPlugins != null && !sessionPlugins.isEmpty()
                ? sessionPlugins
                : configured;
        return HarnessPluginList.intersect(base, allowed);
    }

    private String resolveSystemPrompt(CurrentActor actor) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (actor.isPlatformAdmin() && tenantId == null) {
            return requirePlatformConfig().getSystemPrompt();
        }
        return requireTenantConfig(tenantId).getSystemPrompt();
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
        if (actor.isPlatformAdmin() && ActorAccess.tenantId(actor) == null) {
            return true;
        }
        return ActorAccess.tenantId(actor) != null
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
        if (ActorAccess.tenantId(actor) == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireTenantRead(CurrentActor actor) {
        if (ActorAccess.tenantId(actor) == null) {
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
        if (usage == null || ActorAccess.tenantId(actor) == null) {
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
