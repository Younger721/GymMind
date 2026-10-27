package com.gymmind.ai.harness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gymmind.ai.harness")
public class AiHarnessProperties {

    /** 回答路径：AUTO 按需路由；LEGACY_ALWAYS_RAG 每轮先检索（旧行为） */
    private RoutingMode routingMode = RoutingMode.AUTO;

    /** 模糊问句下店长是否同时开放 RAG 与工具 */
    private boolean hybridOnAmbiguous = true;

    /** KNOWLEDGE_ONLY 且无检索片段时的策略 */
    private EmptyKnowledgePolicy emptyKnowledgePolicy = EmptyKnowledgePolicy.DISCLOSE;

    /** 平台管理员默认可用插件 */
    private List<String> platformDefaultPlugins = new ArrayList<>(List.of("basic_chat", "knowledge_search"));

    /** 新租户开通时默认插件 */
    private List<String> tenantDefaultPlugins = new ArrayList<>(List.of("basic_chat", "knowledge_search"));

    /** 默认系统提示词 */
    private String defaultSystemPrompt = "你是 GymMind 智能健身助手，回答要专业、简洁、安全。";

    public List<String> getPlatformDefaultPlugins() {
        return platformDefaultPlugins;
    }

    public void setPlatformDefaultPlugins(List<String> platformDefaultPlugins) {
        this.platformDefaultPlugins = platformDefaultPlugins;
    }

    public List<String> getTenantDefaultPlugins() {
        return tenantDefaultPlugins;
    }

    public void setTenantDefaultPlugins(List<String> tenantDefaultPlugins) {
        this.tenantDefaultPlugins = tenantDefaultPlugins;
    }

    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    public void setDefaultSystemPrompt(String defaultSystemPrompt) {
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    public RoutingMode getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(RoutingMode routingMode) {
        this.routingMode = routingMode == null ? RoutingMode.AUTO : routingMode;
    }

    public boolean isHybridOnAmbiguous() {
        return hybridOnAmbiguous;
    }

    public void setHybridOnAmbiguous(boolean hybridOnAmbiguous) {
        this.hybridOnAmbiguous = hybridOnAmbiguous;
    }

    public EmptyKnowledgePolicy getEmptyKnowledgePolicy() {
        return emptyKnowledgePolicy;
    }

    public void setEmptyKnowledgePolicy(EmptyKnowledgePolicy emptyKnowledgePolicy) {
        this.emptyKnowledgePolicy = emptyKnowledgePolicy == null
                ? EmptyKnowledgePolicy.DISCLOSE : emptyKnowledgePolicy;
    }

    public enum RoutingMode {
        AUTO,
        LEGACY_ALWAYS_RAG
    }

    public enum EmptyKnowledgePolicy {
        /** 仍生成回答，并说明知识库无依据 */
        DISCLOSE,
        /** 无片段则拒答 */
        REFUSE
    }
}
