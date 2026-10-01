package com.gymmind.ai.harness;

/** 固定的系统执行状态，不包含模型思考内容。 */
public record AiProgressEvent(
        String pluginId,
        String stage,
        Status status,
        String message,
        Integer resultCount) {

    public enum Status {
        STARTED,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
