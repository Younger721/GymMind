package com.gymmind.iam.api;

import com.gymmind.iam.api.response.RoleResponse;
import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final UserAdministrationService service;
    private final CurrentActorProvider actors;

    public RoleController(UserAdministrationService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list() {
        CurrentActor actor = actors.requireCurrent();
        if (actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.success(service.listRoles(actor).stream().map(RoleResponse::from).toList());
    }
}
