package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;

public interface AiUsageQueryService {
    List<AiUsageRecord> list(CurrentActor actor);
}
