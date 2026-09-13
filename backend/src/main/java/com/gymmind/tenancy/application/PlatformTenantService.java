package com.gymmind.tenancy.application;

import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.command.CreateTenantCommand;
import com.gymmind.tenancy.api.response.TenantView;
import org.springframework.data.domain.Pageable;

public interface PlatformTenantService {

    TenantView create(CreateTenantCommand command, CurrentActor actor);

    PageResponse<TenantView> list(CurrentActor actor, Pageable pageable);

    TenantView activate(Long tenantId, CurrentActor actor);

    TenantView disable(Long tenantId, CurrentActor actor);
}
