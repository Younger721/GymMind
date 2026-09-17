package com.gymmind.agent.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_agent")
public class TenantAgent extends TenantScopedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgentStatus status;

    @Column(name = "knowledge_enabled", nullable = false)
    private boolean knowledgeEnabled = true;

    /** 逗号分隔的 MCP 工具名，如 knowledge_search,search_articles */
    @Column(name = "enabled_tools", nullable = false, length = 512)
    private String enabledTools = "knowledge_search";

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    protected TenantAgent() {
    }

    private TenantAgent(Long tenantId, String name, String description, String systemPrompt, Long createdByUserId) {
        super(tenantId);
        this.name = requireText(name, "Agent name");
        this.description = description == null ? "" : description.trim();
        this.systemPrompt = requireText(systemPrompt, "System prompt");
        this.status = AgentStatus.ACTIVE;
        this.createdByUserId = createdByUserId;
    }

    public static TenantAgent create(Long tenantId, String name, String description, String systemPrompt,
                                     boolean knowledgeEnabled, String enabledTools, Long createdByUserId) {
        TenantAgent agent = new TenantAgent(tenantId, name, description, systemPrompt, createdByUserId);
        agent.knowledgeEnabled = knowledgeEnabled;
        agent.enabledTools = normalizeTools(enabledTools);
        return agent;
    }

    public void update(String name, String description, String systemPrompt, boolean knowledgeEnabled,
                       String enabledTools) {
        this.name = requireText(name, "Agent name");
        this.description = description == null ? "" : description.trim();
        this.systemPrompt = requireText(systemPrompt, "System prompt");
        this.knowledgeEnabled = knowledgeEnabled;
        this.enabledTools = normalizeTools(enabledTools);
    }

    public void disable() {
        status = AgentStatus.DISABLED;
    }

    public void activate() {
        status = AgentStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == AgentStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public boolean isKnowledgeEnabled() {
        return knowledgeEnabled;
    }

    public String getEnabledTools() {
        return enabledTools;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeTools(String tools) {
        if (tools == null || tools.isBlank()) {
            return "knowledge_search";
        }
        return tools.trim();
    }
}
