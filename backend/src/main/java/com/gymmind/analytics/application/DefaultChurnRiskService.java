package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.*;
import com.gymmind.shared.error.*;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import org.springframework.stereotype.Service;
@Service public class DefaultChurnRiskService implements ChurnRiskService {
    private final ChurnRiskCalculator calculator;
    public DefaultChurnRiskService() { this(new ChurnRiskCalculator()); }
    DefaultChurnRiskService(ChurnRiskCalculator calculator) { this.calculator = calculator; }
    public ChurnRiskResult assess(CurrentActor actor, ChurnSignals signals) {
        if (ActorAccess.tenantId(actor) == null || !actor.hasPermission("analytics:read")
                || !actor.roles().contains(RoleCode.GYM_ADMIN)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (signals == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        return calculator.calculate(signals);
    }
}
