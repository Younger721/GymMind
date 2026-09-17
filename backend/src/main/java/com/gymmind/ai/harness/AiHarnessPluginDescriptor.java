package com.gymmind.ai.harness;

/**
 * Harness 插件元数据（类似 DeepSeek Harness 的可选能力单元）
 */
public record AiHarnessPluginDescriptor(
        String id,
        String name,
        String description,
        String category,
        String permission,
        boolean requiresTenant,
        boolean defaultEnabled) {
}
