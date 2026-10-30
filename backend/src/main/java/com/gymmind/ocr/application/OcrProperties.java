package com.gymmind.ocr.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** OCR 模块配置；与 {@code application.yml} 的 {@code gymmind.ocr} 及环境变量对齐 */
@ConfigurationProperties(prefix = "gymmind.ocr")
public class OcrProperties {

    /** 功能总开关（关闭时装配降级适配器） */
    private boolean enabled = true;

    /** 单张图片大小上限（与知识库 10MB 上限对齐） */
    private long maxBytes = 10 * 1024 * 1024L;

    /** 单次识别请求整体超时（毫秒） */
    private long timeoutMs = 60_000L;

    /** 每租户每日识别次数上限（配额计数不含失败任务） */
    private int dailyQuotaPerTenant = 200;

    /** 上传后识别有效期（小时），过期需重新上传 */
    private int uploadTtlHours = 24;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = Math.max(1024, maxBytes);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    public int getDailyQuotaPerTenant() {
        return dailyQuotaPerTenant;
    }

    public void setDailyQuotaPerTenant(int dailyQuotaPerTenant) {
        this.dailyQuotaPerTenant = Math.max(1, dailyQuotaPerTenant);
    }

    public int getUploadTtlHours() {
        return uploadTtlHours;
    }

    public void setUploadTtlHours(int uploadTtlHours) {
        this.uploadTtlHours = Math.max(1, uploadTtlHours);
    }
}
