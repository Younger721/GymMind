package com.gymmind.ai.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoachAssistantServiceTest {
    @Test
    void coachRequestIsSentToModelWithMemberContext() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(contains("会员编号=42"), anyList())).thenReturn("建议从低强度恢复训练开始");
        var assignments = mock(com.gymmind.coach.application.CoachAssignmentService.class);
        when(assignments.canAccess(any(), eq(42L))).thenReturn(true);
        var result = new DefaultCoachAssistantService(model, assignments).assist(actor(), 42L, "最近训练频率较低");
        assertThat(result.summary()).contains("低强度");
    }

    @Test
    void memberCannotUseCoachAssistant() {
        var service = new DefaultCoachAssistantService(mock(ChatModelGateway.class), mock(com.gymmind.coach.application.CoachAssignmentService.class));
        var member = new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:analysis"), 0, "t");
        assertThatThrownBy(() -> service.assist(member, 42L, "help")).isInstanceOf(RuntimeException.class);
    }

    private CurrentActor actor() {
        return new CurrentActor(9L, 7L, Set.of(RoleCode.COACH), Set.of("ai:analysis"), 0, "t");
    }
}
