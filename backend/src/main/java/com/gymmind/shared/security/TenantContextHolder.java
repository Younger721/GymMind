package com.gymmind.shared.security;

/**
 * 请求级租户上下文：平台管理员代管门店业务时使用。
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(Long tenantId) {
        if (tenantId == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(tenantId);
        }
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
