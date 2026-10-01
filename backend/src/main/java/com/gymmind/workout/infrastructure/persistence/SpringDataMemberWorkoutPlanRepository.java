package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.MemberWorkoutPlan; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SpringDataMemberWorkoutPlanRepository extends JpaRepository<MemberWorkoutPlan,Long>{ List<MemberWorkoutPlan> findAllByTenantIdAndMemberId(Long tenantId,Long memberId); Optional<MemberWorkoutPlan> findByTenantIdAndId(Long tenantId,Long id); }
