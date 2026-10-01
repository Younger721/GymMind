package com.gymmind.workout.application;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.workout.domain.model.WorkoutRecord;
import com.gymmind.workout.domain.model.WorkoutSet;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import com.gymmind.workout.domain.repository.WorkoutSetRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkoutHistoryQueryServiceTest {

    @Test
    void summarizesScopedCompletedSetsWithoutIncludingFreeTextNotes() {
        WorkoutRecordRepository records = mock(WorkoutRecordRepository.class);
        WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        WorkoutRecord record = mock(WorkoutRecord.class);
        WorkoutSet completedSet = mock(WorkoutSet.class);
        WorkoutSet incompleteSet = mock(WorkoutSet.class);
        Exercise squat = mock(Exercise.class);
        LocalDate since = LocalDate.of(2026, 8, 1);
        LocalDate until = LocalDate.of(2026, 9, 26);

        when(record.getId()).thenReturn(51L);
        when(record.getWorkoutDate()).thenReturn(LocalDate.of(2026, 9, 20));
        when(record.getDuration()).thenReturn(45);
        when(record.getNotes()).thenReturn("private note: phone 123456");
        when(records.findRecentCompleted(7L, 41L, since, until, 30)).thenReturn(List.of(record));
        when(completedSet.getRecord()).thenReturn(record);
        when(completedSet.getExerciseId()).thenReturn(8L);
        when(completedSet.getSetNumber()).thenReturn(1);
        when(completedSet.getWeight()).thenReturn(40.0);
        when(completedSet.getReps()).thenReturn(10);
        when(completedSet.isCompleted()).thenReturn(true);
        when(incompleteSet.getRecord()).thenReturn(record);
        when(incompleteSet.getExerciseId()).thenReturn(8L);
        when(incompleteSet.getSetNumber()).thenReturn(2);
        when(incompleteSet.getWeight()).thenReturn(40.0);
        when(incompleteSet.getReps()).thenReturn(10);
        when(incompleteSet.isCompleted()).thenReturn(false);
        when(sets.findAllByRecordIds(List.of(51L))).thenReturn(List.of(completedSet, incompleteSet));
        when(squat.getId()).thenReturn(8L);
        when(squat.getName()).thenReturn("深蹲");
        when(exercises.findAllByTenantId(7L)).thenReturn(List.of(squat));

        WorkoutHistorySummary summary = new WorkoutHistoryQueryService(records, sets, exercises)
                .summarize(7L, 41L, since, until, 30);

        assertThat(summary.recordCount()).isEqualTo(1);
        assertThat(summary.promptText()).contains("2026-09-20", "深蹲", "40.0kg x 10");
        assertThat(summary.promptText()).doesNotContain("private note", "123456", "incomplete");
        verify(records).findRecentCompleted(7L, 41L, since, until, 30);
        verify(sets).findAllByRecordIds(List.of(51L));
        verify(exercises).findAllByTenantId(7L);
    }

    @Test
    void capsRequestedHistoryAtThirtyRecords() {
        WorkoutRecordRepository records = mock(WorkoutRecordRepository.class);
        WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
        ExerciseRepository exercises = mock(ExerciseRepository.class);
        when(records.findRecentCompleted(eq(7L), eq(41L), any(), any(), eq(30))).thenReturn(List.of());

        WorkoutHistorySummary summary = new WorkoutHistoryQueryService(records, sets, exercises)
                .summarize(7L, 41L, LocalDate.now().minusWeeks(8), LocalDate.now(), 100);

        assertThat(summary.recordCount()).isZero();
        assertThat(summary.promptText()).contains("暂无");
        verifyNoInteractions(sets, exercises);
    }
}
