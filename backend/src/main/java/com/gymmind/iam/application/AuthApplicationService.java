package com.gymmind.iam.application;

import com.gymmind.iam.application.command.LoginCommand;
import com.gymmind.iam.application.command.LogoutCommand;
import com.gymmind.iam.application.command.RefreshCommand;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.shared.security.CurrentActor;

public interface AuthApplicationService {

    AuthResult registerTenant(RegisterTenantCommand command);

    AuthResult login(LoginCommand command);

    AuthResult refresh(RefreshCommand command);

    void logout(CurrentActor actor, LogoutCommand command);
}
