package com.gymmind.ai.chat.domain.model;

import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_chat_session", indexes = {
        @Index(name = "idx_ai_chat_session_scope", columnList = "tenant_id,user_id,role_context,updated_at")
})
public class AiChatSession extends BaseEntity {
    @Column(name="tenant_id") private Long tenantId;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="role_context", nullable=false, length=32) private String roleContext;
    @Column(name="session_key", nullable=false, length=128) private String sessionKey;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @Column(nullable=false) private boolean archived;
    protected AiChatSession() {}
    public AiChatSession(Long tenantId, Long userId, String roleContext, String sessionKey, String title) {
        this.tenantId=tenantId; this.userId=userId; this.roleContext=roleContext; this.sessionKey=sessionKey;
        this.title=title; this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
    public void touch(String title) { if (title != null && !title.isBlank() && "新建对话".equals(this.title)) this.title=title.trim().substring(0, Math.min(160,title.trim().length())); this.updatedAt=Instant.now(); }
    public void archive(){ archived=true; updatedAt=Instant.now(); }
    public Long getTenantId(){return tenantId;} public Long getUserId(){return userId;} public String getRoleContext(){return roleContext;}
    public String getSessionKey(){return sessionKey;} public String getTitle(){return title;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public boolean isArchived(){return archived;}
}
