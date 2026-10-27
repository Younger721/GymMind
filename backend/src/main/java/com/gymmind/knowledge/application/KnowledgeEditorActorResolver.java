package com.gymmind.knowledge.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 从用户 ID 构造编辑器回调用的 CurrentActor（不依赖 JWT） */
@Component
public class KnowledgeEditorActorResolver {

    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;

    public KnowledgeEditorActorResolver(UserAccountRepository users, UserRoleRepository userRoles) {
        this.users = users;
        this.userRoles = userRoles;
    }

    public CurrentActor requireActiveEditorActor(long userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        Set<RoleCode> roles = userRoles.findRoleCodesByUserId(user.getId());
        if (user.getTenantId() == null) {
            if (!roles.contains(RoleCode.PLATFORM_ADMIN)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (!roles.contains(RoleCode.GYM_ADMIN) && !roles.contains(RoleCode.PLATFORM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Set<String> permissions = user.getTenantId() == null
                ? userRoles.findPermissionCodesByUserId(user.getId())
                : userRoles.findPermissionCodesByUserId(user.getId(), user.getTenantId());
        if (permissions == null || permissions.isEmpty()) {
            permissions = Set.of();
        }
        return new CurrentActor(user.getId(), user.getTenantId(), roles, permissions, user.getTokenVersion(), "onlyoffice");
    }
}
