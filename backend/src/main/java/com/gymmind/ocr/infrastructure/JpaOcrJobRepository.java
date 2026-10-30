package com.gymmind.ocr.infrastructure;

import com.gymmind.ocr.application.OcrJobRepository;
import com.gymmind.ocr.domain.model.OcrJob;
import com.gymmind.ocr.domain.model.OcrJobStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/** JPA 仓储实现：领域模型 ↔ 持久化实体映射，对应用层屏蔽 JPA 细节 */
@Repository
public class JpaOcrJobRepository implements OcrJobRepository {

    private final SpringDataOcrJobRepository data;

    public JpaOcrJobRepository(SpringDataOcrJobRepository data) {
        this.data = data;
    }

    @Override
    public OcrJob save(OcrJob job) {
        OcrJobEntity saved = data.save(OcrJobEntity.fromDomain(job));
        job.assignId(saved.getId());
        job.assignCreatedAt(saved.getCreatedAt());
        return job;
    }

    @Override
    public Optional<OcrJob> findByTenantIdAndId(Long tenantId, Long id) {
        OcrJobEntity entity = data.findByTenantIdAndId(tenantId, id);
        return entity == null ? Optional.empty() : Optional.of(toDomain(entity));
    }

    @Override
    public long countActiveSince(Long tenantId, OcrJobStatus excluded, Instant since) {
        return data.countByTenantIdAndStatusNotSince(tenantId, excluded, since);
    }

    /** 实体 → 领域模型 */
    private static OcrJob toDomain(OcrJobEntity e) {
        return OcrJob.rehydrate(e.getId(), e.getTenantId(), e.getCreatedByUserId(), e.getUploadKey(),
                e.getFileName(), e.getContentType(), e.getSizeBytes(), e.getStatus(), e.getPageCount(),
                e.getMarkdown(), e.getProvider(), e.getModel(), e.getInputTokens(), e.getOutputTokens(),
                e.getTotalTokens(), e.getProviderRequestId(), e.getErrorMessage(), e.getCreatedAt());
    }
}
