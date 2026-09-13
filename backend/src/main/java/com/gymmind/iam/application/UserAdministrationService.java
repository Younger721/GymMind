package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserAdministrationService {
    UserView create(CurrentActor actor, CreateUserCommand command);
    PageResponse<UserSummary> list(CurrentActor actor, Pageable pageable);
    void changeStatus(CurrentActor actor, Long userId, UserStatus status);
    void replaceRoles(CurrentActor actor, Long userId, Set<RoleCode> roles);
    List<RoleView> listRoles(CurrentActor actor);

    record UserView(Long id, Long tenantId, String email, String displayName,
                    UserStatus status, Set<RoleCode> roles) {}
    record UserSummary(Long id, Long tenantId, String email, String displayName,
                       UserStatus status, Set<RoleCode> roles) {}
    record RoleView(Long id, RoleCode code, String name) {}
}
