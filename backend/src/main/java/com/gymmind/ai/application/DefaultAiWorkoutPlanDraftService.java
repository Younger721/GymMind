package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaServices;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutHistoryQueryService;
import com.gymmind.workout.application.WorkoutHistorySummary;
import com.gymmind.workout.application.WorkoutPlanItemCommand;
import com.gymmind.workout.application.WorkoutPlanValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultAiWorkoutPlanDraftService implements AiWorkoutPlanDraftService {
    private final ChatModelGateway model;
    private final ObjectMapper mapper;
    private final WorkoutPlanValidator validator = new WorkoutPlanValidator();
    private final TenantQuotaService quotas;
    private final MemberRepository members;
    private final CoachRepository coaches;
    private final CoachAssignmentRepository assignments;
    private final WorkoutHistoryQueryService history;
    private final ExerciseRepository exercises;
    private final DefaultAiUsageService usage;

    /** Kept for existing integrations and unit tests; Spring uses the complete constructor below. */
    public DefaultAiWorkoutPlanDraftService(ChatModelGateway model, ObjectMapper mapper) {
        this(model, mapper, TenantQuotaServices.unboundedForLegacyConstruction(), null, null, null, null, null, null);
    }

    /** Kept for existing integrations and unit tests. */
    public DefaultAiWorkoutPlanDraftService(ChatModelGateway model, ObjectMapper mapper, TenantQuotaService quotas) {
        this(model, mapper, quotas, null, null, null, null, null, null);
    }

    public DefaultAiWorkoutPlanDraftService(ChatModelGateway model, ObjectMapper mapper, TenantQuotaService quotas,
                                            MemberRepository members, CoachRepository coaches,
                                            CoachAssignmentRepository assignments, WorkoutHistoryQueryService history,
                                            ExerciseRepository exercises) {
        this(model, mapper, quotas, members, coaches, assignments, history, exercises, null);
    }

    @Autowired
    public DefaultAiWorkoutPlanDraftService(ChatModelGateway model, ObjectMapper mapper, TenantQuotaService quotas,
                                            MemberRepository members, CoachRepository coaches,
                                            CoachAssignmentRepository assignments, WorkoutHistoryQueryService history,
                                            ExerciseRepository exercises, DefaultAiUsageService usage) {
        this.model = Objects.requireNonNull(model, "model");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.quotas = quotas == null ? TenantQuotaServices.unboundedForLegacyConstruction() : quotas;
        this.members = members;
        this.coaches = coaches;
        this.assignments = assignments;
        this.history = history;
        this.exercises = exercises;
        this.usage = usage;
    }

    @Override
    public CreateWorkoutPlanCommand generate(CurrentActor actor, Long memberId, String request) {
        return generateDraft(actor, memberId, request, true).plan();
    }

    @Override
    public AiWorkoutPlanDraftResponse generateDraft(CurrentActor actor, Long requestedMemberId,
                                                    AiWorkoutPlanDraftRequest draftRequest) {
        if (draftRequest == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return generateDraftInternal(actor, requestedMemberId, draftRequest);
    }

    @Override
    public AiWorkoutPlanDraftResponse generateDraft(CurrentActor actor, Long requestedMemberId,
                                                    String request, boolean includeHistory) {
        return generateDraftInternal(actor, requestedMemberId,
                new AiWorkoutPlanDraftRequest(request, includeHistory, null, null, null));
    }

    private AiWorkoutPlanDraftResponse generateDraftInternal(CurrentActor actor, Long requestedMemberId,
                                                             AiWorkoutPlanDraftRequest draftRequest) {
        String request = draftRequest.request();
        boolean includeHistory = draftRequest.includeHistory();
        Long tenantId = ActorAccess.tenantId(actor);
        if (actor == null || tenantId == null || !actor.hasPermission("ai:plan")
                || !draftRequest.hasAnyInput()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long memberId = resolveTarget(actor, tenantId, requestedMemberId);
        quotas.ensureAiChatAllowed(tenantId);

        WorkoutHistorySummary historySummary = null;
        if (includeHistory && history != null) {
            historySummary = history.summarize(tenantId, memberId,
                    LocalDate.now().minusWeeks(8), LocalDate.now(), 30);
        }
        String prompt = WorkoutPlanPromptComposer.compose(
                request,
                draftRequest.goal(),
                draftRequest.sessionsPerWeek(),
                draftRequest.experience(),
                includeHistory,
                historySummary);
        final String response;
        try {
            long started = System.currentTimeMillis();
            ChatModelCallResult call = model.completeWithUsage(prompt, List.of());
            if (call == null) call = ChatModelCallResult.unavailable(model.complete(prompt, List.of()),
                    Math.max(0, System.currentTimeMillis() - started), "OK");
            if (usage != null) {
                try {
                    usage.record(actor, call, "workout-plan");
                } catch (RuntimeException ignored) {
                    // Usage persistence must not replace a generated plan.
                }
            }
            response = call.content();
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        CreateWorkoutPlanCommand plan = parsePlan(response, tenantId, memberId);
        AiWorkoutPlanDraftResponse.History historyView;
        if (!includeHistory) {
            historyView = new AiWorkoutPlanDraftResponse.History(false, 0, "未参考训练记录（用户已关闭）。");
        } else if (historySummary == null) {
            historyView = new AiWorkoutPlanDraftResponse.History(true, 0, "近期暂无可参考训练记录。");
        } else {
            historyView = new AiWorkoutPlanDraftResponse.History(true, historySummary.recordCount(), historySummary.promptText());
        }
        return new AiWorkoutPlanDraftResponse(plan, historyView);
    }

    @Override
    public List<AiWorkoutPlanTarget> targets(CurrentActor actor) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (actor == null || tenantId == null || !actor.hasPermission("ai:plan")
                || !actor.roles().contains(RoleCode.MEMBER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return List.of(target(memberForUser(tenantId, actor.userId())));
    }

    private Long resolveTarget(CurrentActor actor, Long tenantId, Long requestedMemberId) {
        if (!actor.roles().contains(RoleCode.MEMBER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (members == null) {
            // 旧测试构造器：仅允许显式 memberId，且必须是会员角色。
            if (requestedMemberId == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return requestedMemberId;
        }
        Member self = memberForUser(tenantId, actor.userId());
        if (requestedMemberId != null && !Objects.equals(requestedMemberId, self.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return self.getId();
    }

    private Member memberForUser(Long tenantId, Long userId) {
        if (members == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return members.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private CreateWorkoutPlanCommand parsePlan(String response, Long tenantId, Long memberId) {
        try {
            JsonNode root = mapper.readTree(stripFences(response));
            List<WorkoutPlanItemCommand> items = new ArrayList<>();
            for (JsonNode item : root.path("items")) {
                Long exerciseId = item.hasNonNull("exerciseId") ? item.path("exerciseId").asLong() : null;
                String exerciseName = item.path("exerciseName").asText(null);
                if ((exerciseName == null || exerciseName.isBlank()) && exerciseId != null && exercises != null) {
                    exerciseName = exercises.findByTenantIdAndId(tenantId, exerciseId).map(Exercise::getName).orElse(null);
                }
                items.add(new WorkoutPlanItemCommand(exerciseId, exerciseName, item.path("dayOfWeek").asInt(),
                        item.path("sets").asInt(), item.path("reps").asInt(), item.path("restSeconds").asInt(),
                        item.path("weight").asDouble(), item.path("notes").asText(""),
                        item.path("phase").asText("MAIN"), item.path("sortOrder").asInt(0)));
            }
            var command = new CreateWorkoutPlanCommand(tenantId, memberId, null,
                    root.path("name").asText(), root.path("goal").asText(),
                    LocalDate.parse(root.path("startDate").asText()), LocalDate.parse(root.path("endDate").asText()),
                    root.path("description").asText(), items);
            validator.validate(command);
            return command;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private AiWorkoutPlanTarget target(Member member) {
        return new AiWorkoutPlanTarget(member.getId(), member.getFullName(), member.getMemberNumber());
    }

    private String stripFences(String response) {
        String value = response.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                value = value.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return value;
    }
}
