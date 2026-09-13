package com.gymmind.tenancy.application;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.api.response.TenantSettingsView;
import com.gymmind.tenancy.application.command.UpdateTenantSettingsCommand;

public interface TenantSettingsService {

    TenantSettingsView getCurrent(CurrentActor actor);

    TenantSettingsView updateCurrent(CurrentActor actor, UpdateTenantSettingsCommand command);
}
