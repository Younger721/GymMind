package com.gymmind.nutrition.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.nutrition.domain.repository.NutritionPlanRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NutritionPlanServiceTest {
    @Test
    void memberCanCreatePlanForSelf() {
        NutritionPlanRepository plans = mock(NutritionPlanRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class); when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 2L)).thenReturn(Optional.of(member));
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(member));
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NutritionPlanService service = new DefaultNutritionPlanService(plans, members, mock(AuditRecorder.class));
        NutritionPlanView view = service.create(memberActor(11L), new CreateNutritionPlanCommand(null, 41L, 2200, 165, 220, 73.3, "gain"));
        assertThat(view.memberId()).isEqualTo(41L); verify(plans).save(any());
    }

    @Test
    void rejectsNegativeTargets() {
        NutritionPlanService service = new DefaultNutritionPlanService(mock(NutritionPlanRepository.class), mock(MemberRepository.class), mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.create(memberActor(11L), new CreateNutritionPlanCommand(null, 41L, -1, 0, 0, 0, "bad")))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }
    private static CurrentActor memberActor(long t){return new CurrentActor(2L,t,Set.of(RoleCode.MEMBER),Set.of("nutrition:write"),0L,"token");}
}
