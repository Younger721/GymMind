package com.gymmind.workout.application;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.workout.domain.model.WorkoutRecord;
import com.gymmind.workout.domain.model.WorkoutSet;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import com.gymmind.workout.domain.repository.WorkoutSetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkoutHistoryQueryService {
    private static final int MAX_RECORDS = 30;

    private final WorkoutRecordRepository records;
    private final WorkoutSetRepository sets;
    private final ExerciseRepository exercises;

    public WorkoutHistoryQueryService(WorkoutRecordRepository records,
                                      WorkoutSetRepository sets,
                                      ExerciseRepository exercises) {
        this.records = records;
        this.sets = sets;
        this.exercises = exercises;
    }

    public WorkoutHistorySummary summarize(Long tenantId, Long memberId,
                                           LocalDate since, LocalDate until, int limit) {
        if (tenantId == null || memberId == null || since == null || until == null || since.isAfter(until)) {
            return new WorkoutHistorySummary(0, "近期暂无已完成训练记录。");
        }
        int boundedLimit = limit <= 0 ? MAX_RECORDS : Math.min(limit, MAX_RECORDS);
        List<WorkoutRecord> recent = records.findRecentCompleted(tenantId, memberId, since, until, boundedLimit);
        if (recent == null || recent.isEmpty()) {
            return new WorkoutHistorySummary(0, "近期暂无已完成训练记录。");
        }
        if (recent.size() > boundedLimit) {
            recent = recent.subList(0, boundedLimit);
        }

        List<Long> recordIds = recent.stream()
                .map(WorkoutRecord::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<WorkoutSet> historySets = recordIds.isEmpty() ? List.of() : sets.findAllByRecordIds(recordIds);
        Map<Long, String> exerciseNames = exerciseNames(tenantId);
        Map<Long, List<WorkoutSet>> setsByRecord = new HashMap<>();
        for (WorkoutSet set : safeList(historySets)) {
            if (set != null && set.getRecord() != null && set.isCompleted()) {
                Long recordId = set.getRecord().getId();
                if (recordId != null) {
                    setsByRecord.computeIfAbsent(recordId, ignored -> new ArrayList<>()).add(set);
                }
            }
        }

        StringBuilder prompt = new StringBuilder("近期已完成训练记录（最多").append(MAX_RECORDS).append("条）：\n");
        for (WorkoutRecord record : recent) {
            if (record == null) {
                continue;
            }
            prompt.append("- ").append(record.getWorkoutDate())
                    .append("，时长").append(record.getDuration()).append("分钟");
            List<WorkoutSet> recordSets = setsByRecord.getOrDefault(record.getId(), List.of());
            recordSets.stream()
                    .sorted(Comparator.comparingInt(WorkoutSet::getSetNumber))
                    .forEach(set -> prompt.append("；")
                            .append(exerciseNames.getOrDefault(set.getExerciseId(), "动作#" + set.getExerciseId()))
                            .append(" ").append(String.format(java.util.Locale.ROOT, "%.1fkg x %d", set.getWeight(), set.getReps())));
            prompt.append("\n");
        }
        return new WorkoutHistorySummary(recent.size(), prompt.toString().trim());
    }

    private Map<Long, String> exerciseNames(Long tenantId) {
        Map<Long, String> names = new HashMap<>();
        if (tenantId == null) {
            return names;
        }
        for (Exercise exercise : safeList(exercises.findAllByTenantId(tenantId))) {
            if (exercise != null && exercise.getId() != null) {
                names.put(exercise.getId(), exercise.getName());
            }
        }
        return names;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
