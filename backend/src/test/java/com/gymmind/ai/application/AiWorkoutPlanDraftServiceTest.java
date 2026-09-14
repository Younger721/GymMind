package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AiWorkoutPlanDraftServiceTest {
    @Test
    void parsesAndValidatesModelPlanBeforeReturningDraft() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn("{\"name\":\"减脂计划\",\"goal\":\"减脂\",\"startDate\":\"2026-09-15\",\"endDate\":\"2026-09-29\",\"description\":\"每周三练\",\"items\":[{\"exerciseId\":1,\"dayOfWeek\":1,\"sets\":3,\"reps\":10,\"restSeconds\":60,\"weight\":0,\"notes\":\"\"}]}");
        var service = new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper());

        CreateWorkoutPlanCommand draft = service.generate(actor(), 42L, "我想减脂，每周训练三次");

        assertThat(draft.memberId()).isEqualTo(42L);
        assertThat(draft.items()).hasSize(1);
        assertThat(draft.items().get(0).sets()).isEqualTo(3);
    }

    @Test
    void rejectsModelPlanWithInvalidTrainingValues() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(any(), anyList())).thenReturn("{\"name\":\"错误计划\",\"goal\":\"减脂\",\"startDate\":\"2026-09-15\",\"endDate\":\"2026-09-29\",\"description\":\"x\",\"items\":[{\"exerciseId\":1,\"dayOfWeek\":8,\"sets\":0,\"reps\":0,\"restSeconds\":-1,\"weight\":0,\"notes\":\"\"}]}");
        var service = new DefaultAiWorkoutPlanDraftService(model, new ObjectMapper());

        assertThatThrownBy(() -> service.generate(actor(), 42L, "减脂"))
                .isInstanceOf(BusinessException.class);
    }

    private CurrentActor actor() {
        return new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:plan"), 0, "token");
    }
}
