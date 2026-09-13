package com.gymmind.knowledge.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="tenant_id", nullable=false) private Long tenantId;
    @Column(name="owner_user_id") private Long ownerUserId;
    @Column(nullable=false, length=255) private String fileName;
    @Column(nullable=false, length=128) private String contentType;
    @Column(nullable=false, length=512) private String objectKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private DocumentVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private DocumentStatus status;
    protected KnowledgeDocument() {}
    private KnowledgeDocument(Long tenantId, Long ownerUserId, String fileName, String contentType, DocumentVisibility visibility, Long sequence) {
        if (tenantId == null || tenantId <= 0 || fileName == null || fileName.isBlank() || contentType == null || contentType.isBlank() || visibility == null || sequence == null) throw new IllegalArgumentException("invalid document");
        if (visibility == DocumentVisibility.PRIVATE_USER && (ownerUserId == null || ownerUserId <= 0)) throw new IllegalArgumentException("private document requires owner");
        this.tenantId=tenantId; this.ownerUserId=ownerUserId; this.fileName=fileName.trim(); this.contentType=contentType.trim(); this.visibility=visibility; this.objectKey="tenant/%d/knowledge/%d/%s".formatted(tenantId, sequence, this.fileName); this.status=DocumentStatus.UPLOADING;
    }
    public static KnowledgeDocument create(Long tenantId, Long ownerUserId, String fileName, String contentType, DocumentVisibility visibility, Long sequence) { return new KnowledgeDocument(tenantId,ownerUserId,fileName,contentType,visibility,sequence); }
    public void assignId(Long id) { if (this.id != null || id == null || id <= 0) throw new IllegalStateException("id already assigned"); this.id=id; }
    public void markParsing() { require(DocumentStatus.UPLOADING); status=DocumentStatus.PARSING; }
    public void markReady() { if (status != DocumentStatus.UPLOADING && status != DocumentStatus.PARSING) throw new IllegalStateException("invalid status transition"); status=DocumentStatus.READY; }
    public void tombstone() { if (status == DocumentStatus.DELETED) return; status=DocumentStatus.DELETED; }
    private void require(DocumentStatus expected) { if (status != expected) throw new IllegalStateException("invalid status transition"); }
    public Long id(){return id;} public Long tenantId(){return tenantId;} public Long ownerUserId(){return ownerUserId;} public String fileName(){return fileName;} public String contentType(){return contentType;} public String objectKey(){return objectKey;} public DocumentVisibility visibility(){return visibility;} public DocumentStatus status(){return status;}
}
