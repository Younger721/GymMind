package com.gymmind.ai.harness.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 平台级 Harness 默认配置（单行 id=1） */
@Entity
@Table(name = "platform_ai_harness_config")
public class PlatformHarnessConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "enabled_plugins", nullable = false, length = 2000)
    private String enabledPlugins;

    @Column(name = "system_prompt", nullable = false, length = 4000)
    private String systemPrompt;

    protected PlatformHarnessConfig() {
    }

    public PlatformHarnessConfig(String enabledPlugins, String systemPrompt) {
        this.id = SINGLETON_ID;
        this.enabledPlugins = enabledPlugins;
        this.systemPrompt = systemPrompt;
    }

    public void update(String enabledPlugins, String systemPrompt) {
        this.enabledPlugins = enabledPlugins;
        this.systemPrompt = systemPrompt;
    }

    public Long getId() {
        return id;
    }

    public String getEnabledPlugins() {
        return enabledPlugins;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}
