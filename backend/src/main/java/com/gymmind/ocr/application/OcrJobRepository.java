package com.gymmind.ocr.application;

import com.gymmind.ocr.domain.model.OcrJob;
import com.gymmind.ocr.domain.model.OcrJobStatus;

import java.time.Instant;
import java.util.Optional;

/** OCR 任务仓储端口：对应用层屏蔽持久化细节（抽离时随模块整体搬迁） */
public interface OcrJobRepository {

    OcrJob save(OcrJob job);

    /** 按租户与主键查找（租户隔离：跨租户返回空） */
    Optional<OcrJob> findByTenantIdAndId(Long tenantId, Long id);

    /** 统计指定时间起、非指定状态的任务数（配额计数用） */
    long countActiveSince(Long tenantId, OcrJobStatus excluded, Instant since);
}
