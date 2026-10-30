package com.gymmind.ocr.domain.model;

import java.time.Instant;

/**
 * OCR 任务领域模型：一次"上传图片 → 识别"的完整记录。
 * 同步接口也落任务记录：一是用量审计与配额计数，二是二期切异步任务时表结构与状态机不变。
 */
public class OcrJob {

    private Long id;
    private final Long tenantId;
    private final Long createdByUserId;
    private final String uploadKey;
    private final String fileName;
    private final String contentType;
    private final int sizeBytes;
    private OcrJobStatus status;
    private int pageCount;
    private String markdown;
    private String provider;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private String providerRequestId;
    private String errorMessage;
    private Instant createdAt;

    private OcrJob(Long id, Long tenantId, Long createdByUserId, String uploadKey, String fileName,
                   String contentType, int sizeBytes, OcrJobStatus status, int pageCount, String markdown,
                   String provider, String model, Integer inputTokens, Integer outputTokens, Integer totalTokens,
                   String providerRequestId, String errorMessage, Instant createdAt) {
        this.id = id;
        this.tenantId = requireTenant(tenantId);
        this.createdByUserId = createdByUserId;
        this.uploadKey = requireText(uploadKey, "upload key");
        this.fileName = fileName == null ? "" : fileName.trim();
        this.contentType = contentType == null ? "" : contentType.trim();
        this.sizeBytes = sizeBytes;
        this.status = status == null ? OcrJobStatus.PENDING : status;
        this.pageCount = pageCount;
        this.markdown = markdown;
        this.provider = provider;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.providerRequestId = providerRequestId;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    /** 新建上传任务：初始状态 PENDING，等待识别 */
    public static OcrJob create(Long tenantId, Long createdByUserId, String uploadKey,
                                String fileName, String contentType, int sizeBytes) {
        return new OcrJob(null, tenantId, createdByUserId, uploadKey, fileName, contentType, sizeBytes,
                OcrJobStatus.PENDING, 0, null, null, null, null, null, null, null, null, null);
    }

    /** 重建（从持久化加载）：仅供仓储映射使用 */
    public static OcrJob rehydrate(Long id, Long tenantId, Long createdByUserId, String uploadKey, String fileName,
                                   String contentType, int sizeBytes, OcrJobStatus status, int pageCount, String markdown,
                                   String provider, String model, Integer inputTokens, Integer outputTokens,
                                   Integer totalTokens, String providerRequestId, String errorMessage, Instant createdAt) {
        return new OcrJob(id, tenantId, createdByUserId, uploadKey, fileName, contentType, sizeBytes, status,
                pageCount, markdown, provider, model, inputTokens, outputTokens, totalTokens,
                providerRequestId, errorMessage, createdAt);
    }

    /** 进入识别中状态：仅 PENDING 或 FAILED（失败重试）允许 */
    public void start() {
        if (status != OcrJobStatus.PENDING && status != OcrJobStatus.FAILED) {
            throw new IllegalStateException("只有 PENDING/FAILED 状态的任务才能开始识别: " + status);
        }
        this.status = OcrJobStatus.RUNNING;
    }

    /** 识别成功：落 Markdown 与用量计量 */
    public void succeed(int pageCount, String markdown, String provider, String model,
                        Integer inputTokens, Integer outputTokens, Integer totalTokens, String providerRequestId) {
        if (status != OcrJobStatus.RUNNING) {
            throw new IllegalStateException("只有 RUNNING 状态的任务才能标记成功: " + status);
        }
        this.status = OcrJobStatus.SUCCEEDED;
        this.pageCount = pageCount;
        this.markdown = markdown == null ? "" : markdown;
        this.provider = provider;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.providerRequestId = providerRequestId;
        this.errorMessage = null;
    }

    /** 识别失败：记录简短原因（不泄露密钥/完整响应体） */
    public void fail(String errorMessage) {
        this.status = OcrJobStatus.FAILED;
        this.errorMessage = errorMessage == null ? "未知原因" : errorMessage;
    }

    public Long id() {
        return id;
    }

    /** 仓储持久化后回填主键 */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("任务主键已存在，不允许重复赋值");
        }
        this.id = id;
    }

    public Long tenantId() {
        return tenantId;
    }

    public Long createdByUserId() {
        return createdByUserId;
    }

    public String uploadKey() {
        return uploadKey;
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public int sizeBytes() {
        return sizeBytes;
    }

    public OcrJobStatus status() {
        return status;
    }

    public int pageCount() {
        return pageCount;
    }

    public String markdown() {
        return markdown;
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public Integer inputTokens() {
        return inputTokens;
    }

    public Integer outputTokens() {
        return outputTokens;
    }

    public Integer totalTokens() {
        return totalTokens;
    }

    public String providerRequestId() {
        return providerRequestId;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** 持久化映射回填创建时间 */
    public void assignCreatedAt(Instant createdAt) {
        if (this.createdAt != null) {
            return;
        }
        this.createdAt = createdAt;
    }

    private static Long requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("OCR 任务必须归属租户");
        }
        return tenantId;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
