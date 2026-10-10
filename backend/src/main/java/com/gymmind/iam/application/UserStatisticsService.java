package com.gymmind.iam.application;

import com.gymmind.shared.security.CurrentActor;
import org.springframework.data.domain.Pageable;

public interface UserStatisticsService {
    com.gymmind.shared.api.PageResponse<UserAdministrationService.UserSummary> list(CurrentActor actor, Pageable pageable);
    default com.gymmind.shared.api.PageResponse<UserAdministrationService.UserSummary> list(CurrentActor actor, Pageable pageable, Long tenantId, String displayName) {
        return list(actor, pageable);
    }
    UserStatisticsView statistics(CurrentActor actor);
}
