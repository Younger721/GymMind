package com.gymmind.ai.infrastructure;
import com.gymmind.ai.application.*;
import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
@Entity @Table(name="ai_memory",uniqueConstraints=@UniqueConstraint(name="uk_ai_memory_scope_key",columnNames={"tenant_id","user_id","memory_type","memory_key"}))
class AiMemoryEntity extends AuditableEntity {
 @Column(name="tenant_id",nullable=false,updatable=false) Long tenantId;
 @Column(name="user_id",nullable=false,updatable=false) Long userId;
 @Enumerated(EnumType.STRING) @Column(name="memory_type",nullable=false,length=32,updatable=false) AiMemoryType type;
 @Column(name="memory_key",nullable=false,length=128,updatable=false) String key;
 @Column(name="memory_value",nullable=false,columnDefinition="TEXT") String value;
 protected AiMemoryEntity(){}
 static AiMemoryEntity create(Long t,Long u,AiMemoryType type,String key,String value){var e=new AiMemoryEntity();e.tenantId=t;e.userId=u;e.type=type;e.key=key;e.value=value;return e;}
 AiMemoryEntry toEntry(){return new AiMemoryEntry(type,key,value);}
}
