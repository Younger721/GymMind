package com.gymmind.ocr.application;

import com.gymmind.ai.application.AiUsageCommand;
import com.gymmind.ai.application.DefaultAiUsageService;
import com.gymmind.knowledge.application.KnowledgeObjectStore;
import com.gymmind.ocr.domain.OcrGateway;
import com.gymmind.ocr.domain.OcrOptions;
import com.gymmind.ocr.domain.OcrRequest;
import com.gymmind.ocr.domain.OcrResult;
import com.gymmind.ocr.domain.model.OcrJob;
import com.gymmind.ocr.domain.model.OcrJobStatus;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * OCR 用例实现：上传引用 + 同步识别。
 * <p>编排职责：鉴权（ocr:use + 租户）→ 模块配额 → 每日次数配额 → 对象存储落盘 →
 * 任务状态机 → 网关识别 → 用量计量。二进制只在网关端口出入（先上传后引用约束）。
 */
@Service
public class DefaultOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOcrService.class);
    /** 识别权限码：与 PermissionCatalog 的 menu:knowledge 展开项对应 */
    public static final String PERMISSION = "ocr:use";

    private final OcrJobRepository jobs;
    private final KnowledgeObjectStore store;
    private final OcrGateway gateway;
    private final TenantQuotaService quotas;
    private final OcrProperties props;
    private final DefaultAiUsageService usage;

    public DefaultOcrService(OcrJobRepository jobs,
                             KnowledgeObjectStore store,
                             OcrGateway gateway,
                             TenantQuotaService quotas,
                             OcrProperties props,
                             DefaultAiUsageService usage) {
        this.jobs = jobs;
        this.store = store;
        this.gateway = gateway;
        this.quotas = quotas;
        this.props = props;
        this.usage = usage;
    }

    @Override
    public OcrUploadView upload(CurrentActor actor, String fileName, String contentType, byte[] content) {
        Long tenantId = requireTenantWithPermission(actor);
        // OCR 属于 AI 能力：租户 AI 模块未开通时直接拒绝
        quotas.ensureAiModuleEnabled(tenantId);
        OcrUploadValidator.validate(contentType, content, props.getMaxBytes());
        ensureDailyQuota(tenantId);

        // 原图落对象存储（路径沿用知识库风格），任务登记后返回 uploadId
        String uploadKey = "tenant/" + tenantId + "/ocr/" + UUID.randomUUID() + "/" + sanitize(fileName);
        store.put(uploadKey, content, contentType);
        OcrJob job = OcrJob.create(tenantId, actor.userId(), uploadKey,
                fileName == null ? "image" : fileName, contentType, content.length);
        jobs.save(job);
        log.info("OCR 上传成功: tenantId={}, uploadId={}, size={}", tenantId, job.id(), content.length);
        return new OcrUploadView(job.id(), job.fileName(), job.contentType(), job.sizeBytes());
    }

    @Override
    public OcrResultView recognize(CurrentActor actor, Long uploadId) {
        Long tenantId = requireTenantWithPermission(actor);
        if (uploadId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        // 租户隔离：跨租户的 uploadId 一律按不存在处理（防探测）
        OcrJob job = jobs.findByTenantIdAndId(tenantId, uploadId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureNotExpired(job);

        // 已成功的任务直接返回缓存结果，避免重复消耗模型额度
        if (job.status() == OcrJobStatus.SUCCEEDED) {
            return toView(job);
        }
        if (job.status() == OcrJobStatus.RUNNING) {
            throw new BusinessException(ErrorCode.CONFLICT, "图片正在识别中，请稍后查询");
        }

        byte[] image = store.read(job.uploadKey());
        if (image == null || image.length == 0) {
            job.fail("原图对象已丢失，请重新上传");
            jobs.save(job);
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, "原图对象已丢失，请重新上传");
        }

        long started = System.currentTimeMillis();
        job.start();
        jobs.save(job);
        try {
            OcrResult result = gateway.recognize(
                    new OcrRequest(job.fileName(), job.contentType(), image, OcrOptions.defaults()));
            job.succeed(result.pages().size(), result.markdown(), result.provider(), result.model(),
                    result.usage() == null ? null : result.usage().inputTokens(),
                    result.usage() == null ? null : result.usage().outputTokens(),
                    result.usage() == null ? null : result.usage().totalTokens(),
                    result.usage() == null ? null : result.usage().providerRequestId());
            jobs.save(job);
            recordUsage(actor, result, started);
            log.info("OCR 识别完成: tenantId={}, uploadId={}, model={}", tenantId, uploadId, job.model());
            return toView(job);
        } catch (RuntimeException failure) {
            // 失败状态独立落库（本方法无外层事务），便于失败重试与用量审计
            job.fail(safeHint(failure));
            jobs.save(job);
            if (failure instanceof BusinessException business) {
                throw business;
            }
            throw failure;
        }
    }

    /** 权限校验：必须归属租户且持有 ocr:use 权限 */
    private static Long requireTenantWithPermission(CurrentActor actor) {
        Long tenantId = ActorAccess.requireTenantId(actor);
        if (actor == null || !actor.hasPermission(PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return tenantId;
    }

    /** 每日次数配额：按"当日非失败任务数"计数，失败任务不计入（不打击重试） */
    private void ensureDailyQuota(Long tenantId) {
        long used = jobs.countActiveSince(tenantId, OcrJobStatus.FAILED, startOfUtcDay());
        if (used >= props.getDailyQuotaPerTenant()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "今日 OCR 识别次数已达上限（" + props.getDailyQuotaPerTenant() + " 次），请明天再试");
        }
    }

    /** 上传有效期校验：过期后要求重新上传（对象键可能已被清理） */
    private void ensureNotExpired(OcrJob job) {
        Instant expireAt = job.createdAt().plusSeconds(props.getUploadTtlHours() * 3600L);
        if (job.createdAt() == null || Instant.now().isAfter(expireAt)) {
            throw new BusinessException(ErrorCode.CONFLICT, "上传已过期，请重新上传图片");
        }
    }

    /** 用量计量：scene=ocr，复用统一计费模型，便于对账与限额 */
    private void recordUsage(CurrentActor actor, OcrResult result, long started) {
        try {
            usage.record(actor, new AiUsageCommand(result.model(), result.usage(),
                    Math.max(0, System.currentTimeMillis() - started), "OK", "ocr"));
        } catch (RuntimeException failure) {
            // 用量记录失败不影响识别结果返回，仅告警
            log.warn("OCR 用量记录失败: {}", failure.getMessage());
        }
    }

    private static OcrResultView toView(OcrJob job) {
        List<String> pages = job.markdown() == null ? List.of() : List.of(job.markdown());
        return new OcrResultView(job.id(), job.status().name(), job.markdown() == null ? "" : job.markdown(),
                pages, job.provider(), job.model(), job.inputTokens(), job.outputTokens(), job.totalTokens());
    }

    /** 返回可展示的简短原因，避免泄露密钥或完整响应体 */
    private static String safeHint(Throwable failure) {
        String message = failure.getMessage();
        String hint = message == null || message.isBlank() ? failure.getClass().getSimpleName() : message.trim();
        return hint.length() > 200 ? hint.substring(0, 200) + "…" : hint;
    }

    private static String sanitize(String fileName) {
        String name = fileName == null || fileName.isBlank() ? "source" : fileName.trim();
        // 只保留文件名中的安全字符，防止路径穿越
        String cleaned = name.replaceAll("[^\\w.\\-\\u4e00-\\u9fa5]", "_");
        return cleaned.isBlank() ? "source" : cleaned;
    }

    private static Instant startOfUtcDay() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
