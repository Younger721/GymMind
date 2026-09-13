package com.gymmind.nutrition.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.nutrition.domain.repository.NutritionRecordRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NutritionRecordServiceTest {
    @Test
    void memberCanRecordDailyIntakeForSelf() {
        NutritionRecordRepository records = mock(NutritionRecordRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class); when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 2L)).thenReturn(Optional.of(member));
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NutritionRecordService service = new DefaultNutritionRecordService(records, members, mock(AuditRecorder.class));
        NutritionRecordView view = service.create(memberActor(11L), new CreateNutritionRecordCommand(null, LocalDate.now(), 2100, 150, 200, 70, "ok"));
        assertThat(view.memberId()).isEqualTo(41L); verify(records).save(any());
    }
    private static CurrentActor memberActor(long t){return new CurrentActor(2L,t,Set.of(RoleCode.MEMBER),Set.of("nutrition:write"),0L,"token");}
}
