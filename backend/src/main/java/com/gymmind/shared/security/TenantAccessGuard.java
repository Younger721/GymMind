package com.gymmind.shared.security;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TenantAccessGuard {

    public Long requireTenant(CurrentActor actor) {
        requireActor(actor);
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return actor.tenantId();
    }

    public void requireSameTenant(CurrentActor actor, Long resourceTenantId) {
        Long actorTenantId = requireTenant(actor);
        if (!Objects.equals(actorTenantId, resourceTenantId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public void requirePermission(CurrentActor actor, String permission) {
        requireActor(actor);
        if (!actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireActor(CurrentActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
