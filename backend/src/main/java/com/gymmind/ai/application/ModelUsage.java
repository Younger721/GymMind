package com.gymmind.ai.application;

/** Usage returned by an upstream model request. Null token values mean the provider did not return usage. */
public record ModelUsage(
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        String providerRequestId,
        boolean available) {

    public static ModelUsage unavailable(String provider, String model, String requestId) {
        return new ModelUsage(provider, model, null, null, null, requestId, false);
    }
}
