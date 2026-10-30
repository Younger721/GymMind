package com.gymmind.ocr.infrastructure;

import com.gymmind.ocr.domain.model.OcrJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/** Spring Data 仓储：OCR 任务表的原子查询 */
public interface SpringDataOcrJobRepository extends JpaRepository<OcrJobEntity, Long> {

    /** 按租户与主键查找（租户隔离：跨租户一律查不到） */
    OcrJobEntity findByTenantIdAndId(Long tenantId, Long id);

    /** 统计指定状态的任务数（配额计数用，如"今日已用次数"） */
    @Query("SELECT COUNT(j) FROM OcrJobEntity j WHERE j.tenantId = :tenantId AND j.status <> :status AND j.createdAt >= :since")
    long countByTenantIdAndStatusNotSince(@Param("tenantId") Long tenantId,
                                          @Param("status") OcrJobStatus status,
                                          @Param("since") Instant since);
}
