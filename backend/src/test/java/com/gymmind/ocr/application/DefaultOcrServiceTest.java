package com.gymmind.ocr.application;

import com.gymmind.ai.application.DefaultAiUsageService;
import com.gymmind.ai.application.ModelUsage;
import com.gymmind.knowledge.application.KnowledgeObjectStore;
import com.gymmind.ocr.domain.OcrGateway;
import com.gymmind.ocr.domain.OcrResult;
import com.gymmind.ocr.domain.model.OcrJob;
import com.gymmind.ocr.domain.model.OcrJobStatus;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** OCR 服务单测：权限、配额、租户隔离、状态机流转与失败落库 */
class DefaultOcrServiceTest {

    private static final CurrentActor ACTOR = new CurrentActor(
            1L, 5L, Set.of(RoleCode.GYM_ADMIN), Set.of("ocr:use"), 0L, "token-1");
    private static final CurrentActor NO_PERM = new CurrentActor(
            2L, 5L, Set.of(RoleCode.MEMBER), Set.of("knowledge:read"), 0L, "token-2");

    private OcrJobRepository jobs;
    private KnowledgeObjectStore store;
    private OcrGateway gateway;
    private TenantQuotaService quotas;
    private DefaultAiUsageService usage;
    private OcrProperties props;
    private DefaultOcrService service;

    @BeforeEach
    void setUp() {
        jobs = Mockito.mock(OcrJobRepository.class);
        store = Mockito.mock(KnowledgeObjectStore.class);
        gateway = Mockito.mock(OcrGateway.class);
        quotas = Mockito.mock(TenantQuotaService.class);
        usage = Mockito.mock(DefaultAiUsageService.class);
        props = new OcrProperties();
        service = new DefaultOcrService(jobs, store, gateway, quotas, props, usage);
        // 仓储保存时回填主键（仅新建任务；重载任务已有主键则原样返回）
        lenient().when(jobs.save(any())).thenAnswer(invocation -> {
            OcrJob job = invocation.getArgument(0);
            if (job.id() == null) {
                job.assignId(99L);
            }
            return job;
        });
        lenient().when(jobs.countActiveSince(anyLong(), any(), any())).thenReturn(0L);
    }

    @Test
    void 无权限上传被拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(NO_PERM, "a.png", "image/png", new byte[]{1, 2, 3}));
        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode());
    }

    @Test
    void 上传落对象存储并登记任务() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        OcrUploadView view = service.upload(ACTOR, "体测单.png", "image/png", png);
        assertEquals(99L, view.uploadId());
        verify(store).put(any(), eq(png), eq("image/png"));
        verify(jobs).save(any());
        verify(quotas).ensureAiModuleEnabled(5L);
    }

    @Test
    void 超过每日配额上传被拒绝() {
        when(jobs.countActiveSince(eq(5L), any(), any())).thenReturn((long) props.getDailyQuotaPerTenant());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(ACTOR, "a.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}));
        assertEquals(ErrorCode.CONFLICT, ex.errorCode());
    }

    @Test
    void 识别成功更新任务并记录用量() {
        OcrJob job = pendingJob();
        when(jobs.findByTenantIdAndId(5L, 7L)).thenReturn(Optional.of(job));
        when(store.read("key-1")).thenReturn(new byte[]{1, 2, 3});
        when(gateway.recognize(any())).thenReturn(new OcrResult(
                List.of(new com.gymmind.ocr.domain.OcrPage(1, "正文", null)),
                "正文", "dashscope", "qwen3.5-ocr", "req-1",
                new ModelUsage("dashscope", "qwen3.5-ocr", 10, 5, 15, "req-1", true)));

        OcrResultView view = service.recognize(ACTOR, 7L);

        assertEquals("SUCCEEDED", view.status());
        assertEquals("正文", view.markdown());
        assertEquals(15, view.totalTokens());
        verify(jobs, Mockito.times(2)).save(job);
        verify(usage).record(eq(ACTOR), any());
    }

    @Test
    void 已成功任务直接返回缓存不重复计费() {
        OcrJob job = OcrJob.rehydrate(7L, 5L, 1L, "key-1", "a.png", "image/png", 3,
                OcrJobStatus.SUCCEEDED, 1, "缓存文本", "dashscope", "qwen3.5-ocr", 1, 1, 2, "r", null,
                Instant.now());
        when(jobs.findByTenantIdAndId(5L, 7L)).thenReturn(Optional.of(job));

        OcrResultView view = service.recognize(ACTOR, 7L);

        assertEquals("缓存文本", view.markdown());
        verify(gateway, never()).recognize(any());
        verify(usage, never()).record(any(), any());
    }

    @Test
    void 跨租户识别按不存在处理() {
        when(jobs.findByTenantIdAndId(5L, 7L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(ACTOR, 7L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.errorCode());
    }

    @Test
    void 识别失败落库FAILED并原样抛出() {
        OcrJob job = pendingJob();
        when(jobs.findByTenantIdAndId(5L, 7L)).thenReturn(Optional.of(job));
        when(store.read("key-1")).thenReturn(new byte[]{1});
        when(gateway.recognize(any())).thenThrow(
                new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, "模型不可用"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(ACTOR, 7L));

        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
        assertEquals(OcrJobStatus.FAILED, job.status());
        assertTrue(job.errorMessage().contains("模型不可用"));
    }

    @Test
    void 过期上传要求重新上传() {
        OcrJob job = OcrJob.rehydrate(7L, 5L, 1L, "key-1", "a.png", "image/png", 3,
                OcrJobStatus.PENDING, 0, null, null, null, null, null, null, null, null,
                Instant.now().minusSeconds(props.getUploadTtlHours() * 3600L + 60));
        when(jobs.findByTenantIdAndId(5L, 7L)).thenReturn(Optional.of(job));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recognize(ACTOR, 7L));
        assertEquals(ErrorCode.CONFLICT, ex.errorCode());
    }

    /** 构造一个刚上传完、待识别的任务 */
    private static OcrJob pendingJob() {
        return OcrJob.rehydrate(7L, 5L, 1L, "key-1", "a.png", "image/png", 3,
                OcrJobStatus.PENDING, 0, null, null, null, null, null, null, null, null, Instant.now());
    }
}
