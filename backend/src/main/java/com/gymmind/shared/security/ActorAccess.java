package com.gymmind.shared.security;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;

/**
 * 统一鉴权入口：平台管理员白名单 + 租户上下文解析。
 */
public final class ActorAccess {

    private ActorAccess() {}

    /** 平台管理员拥有全部权限（白名单） */
    public static boolean isSuperAdmin(CurrentActor actor) {
        return actor != null && actor.isPlatformAdmin();
    }

    /** 解析有效租户 ID：租户用户取自身 tenantId，平台管理员取请求上下文 */
    public static Long tenantId(CurrentActor actor) {
        if (actor == null) {
            return null;
        }
        if (actor.tenantId() != null) {
            return actor.tenantId();
        }
        if (isSuperAdmin(actor)) {
            return TenantContextHolder.get();
        }
        return null;
    }

    public static Long requireTenantId(CurrentActor actor) {
        Long tenantId = tenantId(actor);
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return tenantId;
    }

    public static void requireGymAdmin(CurrentActor actor, String permission) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (isSuperAdmin(actor)) {
            requireTenantId(actor);
            return;
        }
        if (actor.tenantId() == null
                || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public static boolean canManageTenant(CurrentActor actor) {
        return actor != null && (isSuperAdmin(actor) || actor.roles().contains(RoleCode.GYM_ADMIN));
    }
}
