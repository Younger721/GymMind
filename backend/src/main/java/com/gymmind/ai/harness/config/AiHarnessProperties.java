package com.gymmind.ai.harness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gymmind.ai.harness")
public class AiHarnessProperties {

    /** 平台管理员默认可用插件 */
    private List<String> platformDefaultPlugins = new ArrayList<>(List.of("basic_chat"));

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
}
