package com.gymmind.ai.chat.domain.model;

import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="ai_chat_message", uniqueConstraints=@UniqueConstraint(name="uk_ai_chat_message_sequence", columnNames={"session_id","sequence_no"}), indexes=@Index(name="idx_ai_chat_message_session", columnList="session_id,sequence_no"))
public class AiChatMessage extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="session_id", nullable=false) private AiChatSession session;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private AiChatMessageRole role;
    @Column(nullable=false, length=12000) private String content;
    @Column(name="sequence_no", nullable=false) private long sequence;
    @Column(nullable=false) private Instant createdAt;
    protected AiChatMessage() {}
    public AiChatMessage(AiChatSession session, AiChatMessageRole role, String content, long sequence){this.session=session;this.role=role;this.content=content;this.sequence=sequence;this.createdAt=Instant.now();}
    public AiChatSession getSession(){return session;} public AiChatMessageRole getRole(){return role;} public String getContent(){return content;} public long getSequence(){return sequence;} public Instant getCreatedAt(){return createdAt;}
}
