package com.gymmind.ocr.infrastructure;

import com.gymmind.ocr.domain.model.OcrJobStatus;
import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** OCR 任务持久化实体，对应表 ocr_job（dev 环境 ddl-auto=update 自动建表） */
@Entity
@Table(name = "ocr_job")
public class OcrJobEntity extends TenantScopedEntity {

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    /** MinIO 原图对象键，如 tenant/5/ocr/{uuid}/source.png */
    @Column(name = "upload_key", nullable = false, length = 512)
    private String uploadKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OcrJobStatus status;

    @Column(name = "page_count")
    private int pageCount;

    /** 识别出的 Markdown 文本（图片文本量通常远小于 64KB，TEXT 足够） */
    @Lob
    @Column(name = "markdown", columnDefinition = "TEXT")
    private String markdown;

    @Column(length = 32)
    private String provider;

    @Column(length = 64)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "provider_request_id", length = 128)
    private String providerRequestId;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    protected OcrJobEntity() {
    }

    /** JPA 实体构造：租户 ID 交给父类私有字段 */
    protected OcrJobEntity(Long tenantId) {
        super(tenantId);
    }

    /** 从领域模型构建持久化实体（新建时 id 由数据库生成） */
    public static OcrJobEntity fromDomain(com.gymmind.ocr.domain.model.OcrJob job) {
        OcrJobEntity entity = new OcrJobEntity(job.tenantId());
        entity.createdByUserId = job.createdByUserId();
        entity.uploadKey = job.uploadKey();
        entity.fileName = job.fileName();
        entity.contentType = job.contentType();
        entity.sizeBytes = job.sizeBytes();
        entity.status = job.status();
        entity.pageCount = job.pageCount();
        entity.markdown = job.markdown();
        entity.provider = job.provider();
        entity.model = job.model();
        entity.inputTokens = job.inputTokens();
        entity.outputTokens = job.outputTokens();
        entity.totalTokens = job.totalTokens();
        entity.providerRequestId = job.providerRequestId();
        entity.errorMessage = job.errorMessage();
        return entity;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public String getUploadKey() {
        return uploadKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public OcrJobStatus getStatus() {
        return status;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getMarkdown() {
        return markdown;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
