package com.gymmind.knowledge.domain.model;

import com.gymmind.iam.domain.model.RoleCode;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    // Platform documents are shared across tenants and intentionally have no tenant id.
    @Column(name="tenant_id", nullable=true) private Long tenantId;
    @Column(name="owner_user_id") private Long ownerUserId;
    @Column(name="uploader_user_id") private Long uploaderUserId;
    @Column(nullable=false, length=255) private String fileName;
    @Column(nullable=false, length=128) private String contentType;
    @Column(nullable=false, length=512) private String objectKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private DocumentVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private DocumentStatus status;
    @Column(nullable=false, length=32) private String category = "GENERAL";
    @Column(nullable=false) private boolean published = false;
    @Column(name="knowledge_key", length=128) private String knowledgeKey;
    @Column(name="knowledge_version", nullable=false) private int knowledgeVersion = 1;
    /**
     * The strategy is nullable at the database level so records created before
     * structured chunking was introduced remain readable. Accessors expose the
     * compatibility default for those records.
     */
    @Column(name = "chunk_strategy", length = 32)
    private String chunkStrategy = "GENERAL";
    @Column(name = "chunk_strategy_version")
    private Integer chunkStrategyVersion = 1;
    @ElementCollection(fetch = FetchType.EAGER, targetClass = RoleCode.class)
    @CollectionTable(name = "knowledge_document_audience_role", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role_code", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<RoleCode> audienceRoles = new LinkedHashSet<>();
    protected KnowledgeDocument() {}
    private KnowledgeDocument(Long tenantId, Long ownerUserId, String fileName, String contentType, DocumentVisibility visibility, String keySegment) {
        if ((tenantId != null && tenantId <= 0) || fileName == null || fileName.isBlank() || contentType == null || contentType.isBlank() || visibility == null || keySegment == null || keySegment.isBlank()) throw new IllegalArgumentException("invalid document");
        if (visibility == DocumentVisibility.PRIVATE_USER && (ownerUserId == null || ownerUserId <= 0)) throw new IllegalArgumentException("private document requires owner");
        this.tenantId=tenantId; this.ownerUserId=ownerUserId; this.fileName=safeFileName(fileName); this.contentType=contentType.trim(); this.visibility=visibility; this.audienceRoles = new LinkedHashSet<>(tenantId == null
                ? Set.of(RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER)
                : Set.of(RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER)); this.objectKey=(tenantId == null ? "platform" : "tenant/%d".formatted(tenantId))+"/knowledge/%s/%s".formatted(keySegment.trim(), this.fileName); this.status=DocumentStatus.UPLOADING;
    }
    public static KnowledgeDocument create(Long tenantId, Long ownerUserId, String fileName, String contentType, DocumentVisibility visibility, Long sequence) { return new KnowledgeDocument(tenantId,ownerUserId,fileName,contentType,visibility,sequence == null ? null : String.valueOf(sequence)); }
    public static KnowledgeDocument create(Long tenantId, Long ownerUserId, String fileName, String contentType, DocumentVisibility visibility, String keySegment) { return new KnowledgeDocument(tenantId,ownerUserId,fileName,contentType,visibility,keySegment); }
    public void assignId(Long id) { if (this.id != null || id == null || id <= 0) throw new IllegalStateException("id already assigned"); this.id=id; }
    public void recordUploader(Long userId) { if (userId == null || userId <= 0) throw new IllegalArgumentException("uploader required"); uploaderUserId = userId; }
    public void markParsing() { if (status != DocumentStatus.UPLOADING && status != DocumentStatus.READY && status != DocumentStatus.FAILED && status != DocumentStatus.PARSING) throw new IllegalStateException("invalid status transition"); status=DocumentStatus.PARSING; }
    public void markReady() { if (status != DocumentStatus.UPLOADING && status != DocumentStatus.PARSING) throw new IllegalStateException("invalid status transition"); status=DocumentStatus.READY; }
    public void markFailed() { if (status != DocumentStatus.PARSING && status != DocumentStatus.UPLOADING) throw new IllegalStateException("invalid status transition"); status=DocumentStatus.FAILED; }
    public void tombstone() { if (status == DocumentStatus.DELETED) return; status=DocumentStatus.DELETED; }

    /** 更新可见范围（租户隔离内的元数据修改） */
    public void updateVisibility(DocumentVisibility newVisibility, Long actorUserId) {
        if (newVisibility == null) {
            throw new IllegalArgumentException("visibility must not be null");
        }
        if (newVisibility == DocumentVisibility.PRIVATE_USER
                && (actorUserId == null || actorUserId <= 0)) {
            throw new IllegalArgumentException("private document requires owner");
        }
        visibility = newVisibility;
        ownerUserId = newVisibility == DocumentVisibility.PRIVATE_USER ? actorUserId : null;
    }
    public void updateCategory(String newCategory) { if (newCategory == null || newCategory.isBlank()) throw new IllegalArgumentException("category required"); category = newCategory.trim().toUpperCase(); }
    public void assignVersion(String key, int version) { if (key == null || key.isBlank() || version <= 0) throw new IllegalArgumentException("invalid knowledge version"); knowledgeKey = key.trim(); knowledgeVersion = version; }
    public void updateChunkStrategy(String newStrategy, int newVersion) {
        if (newStrategy == null || newStrategy.isBlank()) {
            throw new IllegalArgumentException("chunk strategy required");
        }
        if (newVersion <= 0) {
            throw new IllegalArgumentException("chunk strategy version must be positive");
        }
        chunkStrategy = newStrategy.trim().toUpperCase();
        chunkStrategyVersion = newVersion;
    }
    public void updateAudienceRoles(Set<RoleCode> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("audience roles required");
        }
        audienceRoles = new LinkedHashSet<>(roles);
    }
    public void publish() { if (status != DocumentStatus.READY) throw new IllegalStateException("only ready document can publish"); published = true; }
    public void unpublish() { published = false; }
    private void require(DocumentStatus expected) { if (status != expected) throw new IllegalStateException("invalid status transition"); }
    private static String safeFileName(String raw) {
        String normalized = raw.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String base = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        base = base.trim();
        if (base.isBlank() || ".".equals(base) || "..".equals(base)) {
            throw new IllegalArgumentException("invalid document file name");
        }
        StringBuilder safe = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i++) {
            char ch = base.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_') {
                safe.append(ch);
            } else {
                safe.append('_');
            }
        }
        String result = safe.toString();
        if (result.isBlank() || ".".equals(result) || "..".equals(result)) {
            throw new IllegalArgumentException("invalid document file name");
        }
        return result.length() <= 255 ? result : result.substring(0, 255);
    }
    public Long id(){return id;} public Long tenantId(){return tenantId;} public Long ownerUserId(){return ownerUserId;} public Long uploaderUserId(){return uploaderUserId;} public String fileName(){return fileName;} public String contentType(){return contentType;} public String objectKey(){return objectKey;} public DocumentVisibility visibility(){return visibility;} public DocumentStatus status(){return status;} public String category(){return category;} public boolean published(){return published;} public String knowledgeKey(){return knowledgeKey;} public int knowledgeVersion(){return knowledgeVersion;}
    public Set<RoleCode> audienceRoles(){return Set.copyOf(audienceRoles == null ? Set.of() : audienceRoles);}
    public Set<RoleCode> effectiveAudienceRoles(){
        if (audienceRoles != null && !audienceRoles.isEmpty()) return audienceRoles();
        return tenantId == null
                ? Set.of(RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER)
                : Set.of(RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER);
    }
    public String chunkStrategy(){return chunkStrategy == null || chunkStrategy.isBlank() ? "GENERAL" : chunkStrategy;}
    public int chunkStrategyVersion(){return chunkStrategyVersion == null || chunkStrategyVersion <= 0 ? 1 : chunkStrategyVersion;}
}
