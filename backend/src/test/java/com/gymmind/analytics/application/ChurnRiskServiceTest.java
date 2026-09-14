package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.*; import com.gymmind.iam.domain.model.RoleCode; import com.gymmind.shared.error.BusinessException; import com.gymmind.shared.security.CurrentActor; import org.junit.jupiter.api.Test; import java.util.Set; import static org.assertj.core.api.Assertions.*;
class ChurnRiskServiceTest {
 @Test void requiresAnalyticsPermission(){var s=new DefaultChurnRiskService(); var a=new CurrentActor(1L,7L,Set.of(RoleCode.GYM_ADMIN),Set.of(),0,"t"); assertThatThrownBy(()->s.assess(a,new ChurnSignals(1,1,1,1,false))).isInstanceOf(BusinessException.class);}
 @Test void delegatesRiskCalculationForTenant(){var s=new DefaultChurnRiskService(); var a=new CurrentActor(1L,7L,Set.of(RoleCode.GYM_ADMIN),Set.of("analytics:read"),0,"t"); assertThat(s.assess(a,new ChurnSignals(45,1,8,3,true)).level()).isEqualTo(ChurnRiskLevel.HIGH);}
}
