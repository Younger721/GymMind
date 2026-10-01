package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.MemberWorkoutSchedule; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.*;
public interface SpringDataMemberWorkoutScheduleRepository extends JpaRepository<MemberWorkoutSchedule,Long>{ List<MemberWorkoutSchedule> findAllByTenantIdAndMemberIdAndScheduledDateBetween(Long tenantId,Long memberId,LocalDate from,LocalDate to); Optional<MemberWorkoutSchedule> findByTenantIdAndId(Long tenantId,Long id); }
