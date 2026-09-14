package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.*;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
@Service public class DefaultChurnRiskService implements ChurnRiskService {
    private final ChurnRiskCalculator calculator;
    public DefaultChurnRiskService() { this(new ChurnRiskCalculator()); }
    DefaultChurnRiskService(ChurnRiskCalculator calculator) { this.calculator = calculator; }
    public ChurnRiskResult assess(CurrentActor actor, ChurnSignals signals) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("analytics:read")) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (signals == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        return calculator.calculate(signals);
    }
}
