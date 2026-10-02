package com.gymmind.ai.harness;

import java.util.List;

/** 固定的系统执行状态，不包含模型思考内容。 */
public record AiProgressEvent(
        String pluginId,
        String stage,
        Status status,
        String message,
        Integer resultCount,
        List<AiProgressDetail> details) {

    /** 兼容旧的五参数调用方。 */
    public AiProgressEvent(String pluginId, String stage, Status status,
                           String message, Integer resultCount) {
        this(pluginId, stage, status, message, resultCount, List.of());
    }

    public AiProgressEvent {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public enum Status {
        STARTED,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
