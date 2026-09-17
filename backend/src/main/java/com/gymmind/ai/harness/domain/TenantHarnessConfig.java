package com.gymmind.ai.harness.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 租户级 Harness 插件与提示词配置 */
@Entity
@Table(name = "tenant_ai_harness_config")
public class TenantHarnessConfig {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "enabled_plugins", nullable = false, length = 2000)
    private String enabledPlugins;

    @Column(name = "system_prompt", nullable = false, length = 4000)
    private String systemPrompt;

    protected TenantHarnessConfig() {
    }

    private TenantHarnessConfig(Long tenantId, String enabledPlugins, String systemPrompt) {
        this.tenantId = tenantId;
        this.enabledPlugins = enabledPlugins;
        this.systemPrompt = systemPrompt;
    }

    public static TenantHarnessConfig defaults(Long tenantId, String enabledPlugins, String systemPrompt) {
        return new TenantHarnessConfig(tenantId, enabledPlugins, systemPrompt);
    }

    public void update(String enabledPlugins, String systemPrompt) {
        this.enabledPlugins = enabledPlugins;
        this.systemPrompt = systemPrompt;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getEnabledPlugins() {
        return enabledPlugins;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}
