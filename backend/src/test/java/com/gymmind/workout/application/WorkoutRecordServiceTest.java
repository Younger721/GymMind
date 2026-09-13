package com.gymmind.workout.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import com.gymmind.workout.domain.repository.WorkoutSetRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkoutRecordServiceTest {
    @Test
    void memberCanSaveOwnCompletedRecord() {
        WorkoutRecordRepository records = mock(WorkoutRecordRepository.class);
        WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        var member = mock(com.gymmind.member.domain.model.Member.class);
        when(member.getId()).thenReturn(41L);
        when(members.findByTenantIdAndUserId(11L, 2L)).thenReturn(Optional.of(member));
        when(exercises.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(mock(com.gymmind.exercise.domain.model.Exercise.class)));
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutRecordService service = new DefaultWorkoutRecordService(records, sets, members, exercises,
                mock(AuditRecorder.class));
        WorkoutRecordView view = service.create(member(11L), new CreateWorkoutRecordCommand(null, null,
                LocalDate.now(), 45, 300, "done", List.of(new WorkoutSetCommand(8L, 1, 20.0, 10, 0, true))));

        assertThat(view.tenantId()).isEqualTo(11L);
        verify(records).save(any());
        verify(sets).save(any());
    }

    @Test
    void rejectsNegativeDurationAndReps() {
        WorkoutRecordService service = new DefaultWorkoutRecordService(mock(WorkoutRecordRepository.class),
                mock(WorkoutSetRepository.class), mock(MemberRepository.class), mock(ExerciseRepository.class),
                mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.create(member(11L), new CreateWorkoutRecordCommand(null, 41L,
                LocalDate.now(), -1, 0, "bad", List.of(new WorkoutSetCommand(8L, 1, 20.0, 0, 0, true)))))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    private static CurrentActor member(long tenantId) {
        return new CurrentActor(2L, tenantId, Set.of(RoleCode.MEMBER), Set.of("workout-record:write"), 0L, "token");
    }
}
